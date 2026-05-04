# OIDC + STS + S3 ストリーム中継アップロード実装

良い単位でコミットすること

## Context

現状の画像アップロード（`backend/src/main/kotlin/.../image/PostImageRouting.kt` → `backend/feature/image/.../ImageUploadHandler.kt`）はローカルディスクに直接書き込んでいる。これを、自前OIDCプロバイダで発行したJWTを用いてSTS `AssumeRoleWithWebIdentity` で一時クレデンシャルを取得し、S3 API互換ストレージへストリーム中継アップロードする方式に拡張する。

要件:
- ライブラリ: AWS SDK for Java V2（s3 / sts）+ Nimbus JOSE+JWT
- コード/コメント/変数で「MinIO」表記禁止。通信は標準S3として扱う。
- OIDCプロバイダ最小機能: `/.well-known/openid-configuration` と `/jwks` を公開
- RSA鍵ペアは JWK 文字列で環境変数から読み込む
- 画像アップロードコンテキスト内でJWTを生成（`AssumeRoleWithWebIdentity` 用）
- `ENABLE_S3=true` のとき新規アップロードはS3、既存ローカルファイルは引き続き読める互換性維持

## ストレージ抽象化方針

ローカル書込みとS3アップロードは `ImageStorageGateway` インターフェースで抽象化する。`ImageUploadHandler` および `GetImageRouting` は具象実装を意識せず、`ImageStorageGateway` のみに依存する。これにより `ENABLE_S3` の切り替えはDIコンテナでの差し替えのみで完結し、新しいストレージ追加時の影響範囲も限定できる。

ローカル実装も対称性のため独立モジュール（`backend/feature/image-storage-local`）に切り出す。`feature/image` には orchestration（`ImageUploadHandler`）のみを残す。

### `backend/app/interfaces/.../ImageStorageGateway.kt`（新規）
```kotlin
interface ImageStorageGateway {
    enum class StorageType { LOCAL, S3 }
    val storageType: StorageType

    fun put(request: PutRequest): PutResult

    // 表示用URLを構築する。
    // LOCAL: 既存の /api/image/v1/{displayId} を構築（クライアントは backend 経由で取得）
    // S3:    1時間有効な pre-signed GET URL を返す（クライアントは S3 へ直接アクセス）
    fun buildDisplayUrl(request: BuildUrlRequest): String

    data class PutRequest(
        val userId: UserId,                 // S3 では key prefix とJWT の name クレームに使用
        val relativePath: String,           // 論理的な相対パス（既存の "yearMonth/uuid.ext" 形式）
        val contentType: String,
        val contentLength: Long?,           // S3パスでは必須、ローカルでは任意
        val maxBytes: Long,
        val inputStream: InputStream,
    )

    sealed interface PutResult {
        // relativePath は DB に保存され、後で読み込み時に再利用される
        data class Success(val relativePath: String) : PutResult
        data object PayloadTooLarge : PutResult
        data object Empty : PutResult
        data class Failure(val cause: Throwable) : PutResult
    }

    data class BuildUrlRequest(
        val domain: String,
        val displayId: String,
        val userId: UserId,
        val relativePath: String,
        val purpose: Purpose,        // USER / ADMIN（既存の /api/image と /api/admin_image の使い分けに対応）
    )

    enum class Purpose { USER, ADMIN }
}
```

ローカル経由のファイル配信用に、`LocalImageStorageGateway` は追加で `openInputStream(relativePath: String): ReadResult?` を提供する（`ImageStorageGateway` 本体には含めない、Local固有のAPI）。`userId` はローカル実装では無視（既存ファイルとの互換性のため key 構造を変えない）。

### S3 におけるオブジェクトキー仕様
S3 実装は内部で `key = "img/${userId.value}/${relativePath}"` を組み立てる。
- 書き込み: `PutObjectRequest.key(composedKey)` で送信
- 読み込み (presign): `GetObjectRequest.key(composedKey)`
- 例: bucket=`my-bucket`, userId=`42`, relativePath=`2026-05/abcd.jpg` → S3 key=`img/42/2026-05/abcd.jpg`、URL は `${endpoint}/${bucket}/img/42/2026-05/abcd.jpg?...`（path style 時）

### JWT クレーム仕様
`JwtIssuer.issueWebIdentityToken` が生成する JWT には以下を含める:
- `iss` = `ServerEnv.oidcIssuer`
- `sub` = `userId.value.toString()`
- `aud` = `ObjectStorageConfig.audience`（環境変数 `S3_AUDIENCE`）
- `iat` / `exp` / `jti`
- `name` = `userId.value.toString()` ← ユーザーが要求した、user_id を name クレームに含める
- 必要に応じて `role` 等の追加クレーム（カスタムクレーム）

S3 ストレージ側のIAMポリシーで `name` クレームを利用したパス制限（例: `img/${name}/*` のみ書込可）を将来的に組める形にしておく。

## モジュール構成（新規3モジュール）

### `backend/feature/oidc`（新規）
鍵管理・JWT発行・OIDCエンドポイントを担当。

ファイル:
- `build.gradle.kts` … kotlin-multiplatform、依存: `projects.shared`, `projects.backend.base`, `libs.ktorServerCore`, `libs.kotlin.serialization.json`, `libs.nimbusJoseJwt`
- `src/jvmMain/kotlin/net/matsudamper/money/backend/feature/oidc/OidcKeyManager.kt`
    - `class OidcKeyManager(jwkJson: String)` … 環境変数の JWK JSON を `com.nimbusds.jose.jwk.RSAKey.parse(...)` で読み込み、`kid` と `RSAKey`（private含む）を保持
    - `fun publicJwkSet(): String` … `RSAKey.toPublicJWK()` を `JWKSet` でラップしJSON文字列化
    - `fun getKid(): String`、`fun getPrivateKey()`
- `src/jvmMain/kotlin/net/matsudamper/money/backend/feature/oidc/JwtIssuer.kt`
    - `class JwtIssuer(keyManager: OidcKeyManager, issuer: String)`
    - `fun issueWebIdentityToken(subject: String, name: String, audience: String, ttl: Duration, customClaims: Map<String, Any> = emptyMap()): String`
    - 内部で `JWSHeader.Builder(JWSAlgorithm.RS256).keyID(...)`、`JWTClaimsSet`（iss, sub, aud, exp, iat, jti, name, +カスタム）、`SignedJWT.sign(RSASSASigner(...))`
    - `name` には呼び出し側から `userId.value.toString()` を渡す
- `src/jvmMain/kotlin/net/matsudamper/money/backend/feature/oidc/OidcDiscoveryRouting.kt`
    - `fun Route.oidcDiscovery(issuer: String)` … `GET /.well-known/openid-configuration` で `issuer`, `jwks_uri`, `response_types_supported`, `subject_types_supported`, `id_token_signing_alg_values_supported`(["RS256"]) を返す
- `src/jvmMain/kotlin/net/matsudamper/money/backend/feature/oidc/JwksRouting.kt`
    - `fun Route.jwks(keyManager: OidcKeyManager)` … `GET /jwks` で `keyManager.publicJwkSet()` を `application/json` で返す

### `backend/feature/image-storage-local`（新規）
既存のローカルファイル書込み・読取りロジックを切り出す。

ファイル:
- `build.gradle.kts` … 依存: `projects.shared`, `projects.backend.base`, `projects.backend.app.interfaces`, `projects.backend.feature.image`（`ImageApiPath` を利用）
- `src/jvmMain/kotlin/.../LocalImageStorageGateway.kt`
    - `class LocalImageStorageGateway(storageDirectory: File): ImageStorageGateway`
    - `storageType = LOCAL`
    - `put(...)`: 既存 `ImageUploadHandler.writeImageFile` 相当のロジック（バッファ書込み、サイズ上限チェック、空ファイル検知、失敗時 destination.delete()）を移植
    - `buildDisplayUrl(req)`: `Purpose` に応じて `ImageApiPath.imageV1AbsoluteByDisplayId(domain, displayId)` または `adminImageV1AbsoluteByDisplayId(...)` を返す
    - `openInputStream(key): ReadResult?`: `File(storageDirectory, key)` を `inputStream()` として返す。存在しない場合 null

### `backend/feature/object-storage`（新規）
STS + S3 によるストリームアップロード/ダウンロードを担当。`feature/oidc` に依存。

ファイル:
- `build.gradle.kts` … 依存: `projects.shared`, `projects.backend.base`, `projects.backend.feature.oidc`, `libs.awsS3`, `libs.awsSts`, `libs.kotlin.coroutines.core`
- `src/jvmMain/kotlin/net/matsudamper/money/backend/feature/objectstorage/ObjectStorageConfig.kt`
    - `data class ObjectStorageConfig(val endpoint: String, val region: String, val bucket: String, val roleArn: String, val roleSessionName: String, val audience: String, val pathStyleAccess: Boolean)`
- `src/jvmMain/kotlin/net/matsudamper/money/backend/feature/objectstorage/StsCredentialProvider.kt`
    - `class StsCredentialProvider(jwtIssuer: JwtIssuer, config: ObjectStorageConfig)`
    - `fun assumeWithWebIdentity(userId: UserId, durationSeconds: Int = 7200): AwsSessionCredentials`
    - 内部で `jwtIssuer.issueWebIdentityToken(subject = userId.value.toString(), name = userId.value.toString(), audience = config.audience, ttl = Duration.ofMinutes(5))` を呼び JWT を生成
    - 内部で `StsClient.builder().endpointOverride(URI(config.endpoint)).region(...).credentialsProvider(AnonymousCredentialsProvider.create()).build()`、`AssumeRoleWithWebIdentityRequest.builder().roleArn(...).roleSessionName(...).webIdentityToken(jwt).durationSeconds(durationSeconds).build()` を実行し、レスポンスから `AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken)` を生成
    - pre-signed URLを1時間有効にするため、creds は2時間（余裕を持たせる）以上を要求する
- `src/jvmMain/kotlin/net/matsudamper/money/backend/feature/objectstorage/S3ImageStorageGateway.kt`
    - `class S3ImageStorageGateway(stsCredentialProvider: StsCredentialProvider, config: ObjectStorageConfig): ImageStorageGateway`
    - `storageType = S3`
    - `put(...)`: 一時クレデンシャル取得 → `S3Client.builder().endpointOverride(URI(config.endpoint)).region(Region.of(config.region)).credentialsProvider(StaticCredentialsProvider.create(creds)).serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(config.pathStyleAccess).build()).build()` → `PutObjectRequest.builder().bucket(config.bucket).key(request.key).contentType(request.contentType).contentLength(request.contentLength).build()` と `RequestBody.fromInputStream(request.inputStream, request.contentLength)` で送信。`contentLength == null` の場合は Failure
    - サイズ上限超過は事前に `contentLength > maxBytes` でチェック。`contentLength == 0` は Empty
    - `buildDisplayUrl(req)`: pre-signed GET URL を生成して返す。実装:
        - 一時クレデンシャル（durationSeconds=7200）を取得
        - `S3Presigner.builder().endpointOverride(URI(config.endpoint)).region(Region.of(config.region)).credentialsProvider(StaticCredentialsProvider.create(creds)).serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(config.pathStyleAccess).build()).build()` を構築
        - `presignGetObject(GetObjectPresignRequest.builder().signatureDuration(Duration.ofHours(1)).getObjectRequest(GetObjectRequest.builder().bucket(config.bucket).key(req.key).build()).build())` を呼び `URL` 文字列を返却
        - presigner は use ブロックで close する
    - `Purpose` (USER/ADMIN) は S3 では同じバケットを参照するため URL生成上の差異なし。必要であれば prefix で論理分離（オプション）
    - 各操作毎にSTSで取得した一時クレデンシャルで新規クライアントを構築（短命でOK）。クレデンシャルキャッシュは将来課題

## 既存コードへの統合

### `backend/base/.../ServerEnv.kt`（編集）
追加プロパティ:
- `enableS3: Boolean` … `ENABLE_S3` を bool で読む（デフォルト false）
- `oidcIssuer: String?` … `OIDC_ISSUER`
- `oidcJwkPrivate: String?` … `OIDC_JWK_PRIVATE`（JWK JSON、private含む）
- `s3Endpoint: String?` … `S3_ENDPOINT`
- `s3Region: String?` … `S3_REGION`
- `s3Bucket: String?` … `S3_BUCKET`
- `s3RoleArn: String?` … `S3_ROLE_ARN`
- `s3RoleSessionName: String` … `S3_ROLE_SESSION_NAME`（デフォルト `kake-bo-image-upload`）
- `s3Audience: String?` … `S3_AUDIENCE`
- `s3PathStyleAccess: Boolean` … `S3_PATH_STYLE_ACCESS`（デフォルト true）
- `stsEndpoint: String?` … `STS_ENDPOINT`

### `backend/feature/image/.../ImageUploadHandler.kt`（編集）
ローカル/S3を意識せず、`ImageStorageGateway` のみに依存する。

- `Request` から `storageDirectory: File` を削除し、代わりに `imageStorageGateway: ImageStorageGateway`、`contentLength: Long?` を追加
- 既存 `writeImageFile` メソッドは削除（ローカル実装へ移植）
- relativePath は既存通り `"$yearMonth/$displayId.$extension"` で組み立てる
- `imageStorageGateway.put(PutRequest(userId, relativePath, contentType, contentLength, maxBytes=maxUploadBytes, inputStream))` を呼ぶ
- 結果マッピング: `PutResult.Success` → 既存 `Result.Success`、`PayloadTooLarge` / `Empty` / `Failure` は対応する `Result` に変換し DB の `deleteReserveImage` を呼ぶ
- `UserImageRepository.saveImage` は `storageType: StorageType`（`ImageStorageGateway.StorageType` を再利用 or 新 enum）を受ける。DBスキーマに `storage_type` カラム追加（jOOQ 再生成）
- `Result.Success` に `storageType` を含める

### `backend/app/interfaces/.../UserImageRepository.kt`（編集）
- `enum class StorageType { LOCAL, S3 }`
- `fun saveImage(userId, displayId, relativePath, contentType, storageType): ImageId?`
- `fun getImage(...)` 系で `storageType` を返せるように戻り値拡張

### `backend/datasource/db/.../UserImageRepositoryImpl.kt`（編集）
- DBの user_image テーブルに `storage_type VARCHAR(16) NOT NULL DEFAULT 'LOCAL'` を追加（jOOQ 再生成）
- saveImage / 取得処理に storage_type を反映

### `backend/di/.../DiContainer.kt`（編集）
`MainDiContainer` に以下を追加:
- `createOidcKeyManager(): OidcKeyManager?` … `enableS3 && oidcJwkPrivate != null` のときのみ singleton 生成
- `createJwtIssuer(): JwtIssuer?` … 鍵が無ければ null
- `createWriteImageStorageGateway(): ImageStorageGateway` … `enableS3` で `S3ImageStorageGateway` / `LocalImageStorageGateway` を返す（書き込み用は1つ）
- `createReadImageStorageGateway(storageType: StorageType): ImageStorageGateway` … DBに記録された `storage_type` に応じて返す。LOCALなら必ず Local、S3なら S3（`enableS3=false` 環境ではS3の読み込みは未対応として例外で良い）

### `backend/src/main/kotlin/.../Main.kt`（編集）
`routing { ... }` 内で:
- `MainDiContainer` 経由で `OidcKeyManager` / `JwtIssuer` を取得し、非nullなら `oidcDiscovery(issuer = ServerEnv.oidcIssuer!!)` と `jwks(keyManager)` を登録
- `postImage` / `getImage` に `DiContainer` のみを渡し、内部で `createWriteImageStorageGateway` / `createReadImageStorageGateway` を呼ぶ

### `backend/src/main/kotlin/.../image/PostImageRouting.kt`（編集）
`ImageUploadConfig` から `storageDirectory` を削除（DI コンテナ経由で取得）。`maxUploadBytes` のみ保持。
`ImageUploadHandler.handle(...)` 呼び出しでは `diContainer.createWriteImageStorageGateway()` を渡す。`contentLength` は `part.headers["Content-Length"]?.toLongOrNull()` から取得し Request に渡す。
`Result.Success` のURLは `gateway.buildDisplayUrl(BuildUrlRequest(domain, displayId, key, Purpose.USER))` で構築する（LOCALなら `/api/image/v1/{displayId}`、S3なら1時間有効なpre-signed URL）。

### `backend/src/main/kotlin/.../image/GetImageRouting.kt`（編集）
DBから取得した `storage_type` で分岐:
- LOCAL: `LocalImageStorageGateway.openInputStream(key)` の結果を `call.respondOutputStream { copyTo(...) }` で中継。content-type / content-length のレスポンスヘッダも反映（既存挙動を維持）
- S3: `S3ImageStorageGateway.buildDisplayUrl(...)` で fresh な pre-signed URL を発行し `call.respondRedirect(url, permanent = false)` で302リダイレクト（古いキャッシュURL対策）

### `backend/app/src/jvmMain/kotlin/.../graphql/resolver/ImageResolverImpl.kt` / `QueryResolverImpl.kt`（編集）
画像URL構築箇所（`ImageApiPath.imageV1AbsoluteByDisplayId(...)`、`ImageApiPath.adminImageV1AbsoluteByDisplayId(...)`）を、DBから取得した `storage_type` に応じて `diContainer.createReadImageStorageGateway(storageType).buildDisplayUrl(...)` で置き換える。
- ImageResolverImpl: `Purpose.USER`
- QueryResolverImpl (Admin imageList): `Purpose.ADMIN`
- 取得対象が複数件ある場合、各画像ごとにSTSを叩くと N+1 になる。短期的には許容、最適化として S3 単一の Presigner を1リクエスト分まとめる（同一クレデンシャルで複数URLを生成）案を将来検討。当面は S3 切替時のリクエスト数増を許容する旨をコメント無しで実装。

## 依存追加

### `gradle/libs.versions.toml`（編集）
```
[versions]
awsSdk = "2.30.20"
nimbusJoseJwt = "10.0.2"

[libraries]
awsS3 = { module = "software.amazon.awssdk:s3", version.ref = "awsSdk" }
awsSts = { module = "software.amazon.awssdk:sts", version.ref = "awsSdk" }
nimbusJoseJwt = { module = "com.nimbusds:nimbus-jose-jwt", version.ref = "nimbusJoseJwt" }
```

### `settings.gradle.kts`（編集）
```
include(":backend:feature:oidc")
include(":backend:feature:object-storage")
include(":backend:feature:image-storage-local")
```

### `backend/app/build.gradle.kts` / `backend/build.gradle.kts` / `backend/di/build.gradle.kts`（編集）
- `implementation(projects.backend.feature.oidc)`
- `implementation(projects.backend.feature.objectStorage)`
- `implementation(projects.backend.feature.imageStorageLocal)`

`backend/feature/image/build.gradle.kts` には `implementation(projects.backend.appInterfaces)` のみで具象ストレージ実装は依存させない（抽象に依存）。

## DB マイグレーション

`user_image` テーブルに `storage_type` カラム追加。jOOQ コード生成で取り込み。

## 検証

1. ビルド: `./gradlew :backend:assemble :frontend:app:jsBrowserDevelopmentWebpack :frontend:app:assembleDebug --quiet`
2. フォーマット: `./gradlew ktlintFormat`
3. ローカル動作確認:
    - `ENABLE_S3=false`（デフォルト）でアップロード → 既存通りローカル保存され、URLは `/api/image/v1/{displayId}` で配信される
    - `ENABLE_S3=true` + 各S3関連環境変数を設定 → アップロードがS3経由になり、`storage_type=S3` で記録される。レスポンスURLは pre-signed S3 URL（1時間有効）
    - `GET /.well-known/openid-configuration` が JSON を返す
    - `GET /jwks` が公開鍵JWK Setを返す
    - 既存LOCAL画像と新規S3画像が混在する状態で、GraphQL の Image.url がそれぞれ正しく解決される（LOCAL→backend URL、S3→pre-signed URL）
    - pre-signed URL が1時間後に失効することを確認（直接S3に GET → 1時間以内は200、1時間後は403）
4. JWT検証（手動）: `/jwks` の鍵で発行JWTを検証できる
5. 単体テスト: `OidcKeyManager` の鍵読み込み、`JwtIssuer` の発行JWTのclaimsを Kotest で確認

## 影響を受けるファイル一覧

新規:
- `backend/app/interfaces/src/jvmMain/kotlin/.../ImageStorageGateway.kt`
- `backend/feature/oidc/build.gradle.kts`
- `backend/feature/oidc/src/jvmMain/kotlin/.../OidcKeyManager.kt`
- `backend/feature/oidc/src/jvmMain/kotlin/.../JwtIssuer.kt`
- `backend/feature/oidc/src/jvmMain/kotlin/.../OidcDiscoveryRouting.kt`
- `backend/feature/oidc/src/jvmMain/kotlin/.../JwksRouting.kt`
- `backend/feature/image-storage-local/build.gradle.kts`
- `backend/feature/image-storage-local/src/jvmMain/kotlin/.../LocalImageStorageGateway.kt`
- `backend/feature/object-storage/build.gradle.kts`
- `backend/feature/object-storage/src/jvmMain/kotlin/.../ObjectStorageConfig.kt`
- `backend/feature/object-storage/src/jvmMain/kotlin/.../StsCredentialProvider.kt`
- `backend/feature/object-storage/src/jvmMain/kotlin/.../S3ImageStorageGateway.kt`

編集:
- `gradle/libs.versions.toml`
- `settings.gradle.kts`
- `backend/build.gradle.kts`
- `backend/app/build.gradle.kts`
- `backend/di/build.gradle.kts`
- `backend/feature/image/build.gradle.kts`
- `backend/base/src/jvmMain/java/.../ServerEnv.kt`
- `backend/di/src/jvmMain/kotlin/.../DiContainer.kt`
- `backend/app/interfaces/src/jvmMain/kotlin/.../UserImageRepository.kt`
- `backend/datasource/db/src/jvmMain/kotlin/.../UserImageRepositoryImpl.kt`（および user_image テーブル DDL）
- `backend/feature/image/src/jvmMain/kotlin/.../ImageUploadHandler.kt`
- `backend/src/main/kotlin/.../image/PostImageRouting.kt`
- `backend/src/main/kotlin/.../image/GetImageRouting.kt`
- `backend/src/main/kotlin/.../Main.kt`
- `backend/app/src/jvmMain/kotlin/.../graphql/resolver/ImageResolverImpl.kt`
- `backend/app/src/jvmMain/kotlin/.../graphql/resolver/QueryResolverImpl.kt`
