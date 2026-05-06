package net.matsudamper.money.backend.feature.objectstorage

import java.net.URI
import java.time.Duration
import net.matsudamper.money.backend.app.interfaces.ImageStorageGateway
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload
import software.amazon.awssdk.services.s3.model.CompletedPart
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.UploadPartRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest

class S3ImageStorageGateway(
    private val stsCredentialProvider: StsCredentialProvider,
    private val config: ObjectStorageConfig,
) : ImageStorageGateway {

    private companion object {
        /** S3 マルチパートアップロードの1パートサイズ（最小5MB） */
        const val MULTIPART_PART_SIZE = 5 * 1024 * 1024
    }

    override val storageType: ImageStorageGateway.StorageType = ImageStorageGateway.StorageType.S3

    override fun put(request: ImageStorageGateway.PutRequest): ImageStorageGateway.PutResult {
        return putWithMultipart(request)
    }

    override fun read(request: ImageStorageGateway.ReadRequest): ImageStorageGateway.ReadResult {
        val url = buildDisplayUrl(
            ImageStorageGateway.BuildUrlRequest(
                domain = request.domain,
                displayId = request.displayId,
                userId = request.userId,
                relativePath = request.relativePath,
                purpose = request.purpose,
            ),
        )
        return ImageStorageGateway.ReadResult.RedirectUrl(url)
    }

    private fun buildS3Client(credentials: AwsSessionCredentials): S3Client {
        return S3Client.builder().apply {
            if (config.endpoint.isNotBlank()) {
                endpointOverride(URI(config.endpoint))
            }
            region(Region.of(config.region))
            credentialsProvider(StaticCredentialsProvider.create(credentials))
            serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(config.pathStyleAccess)
                    .build(),
            )
        }.build()
    }

    /**
     * Content-Length が不明なストリームを S3 Multipart Upload API で送信する。
     * [MULTIPART_PART_SIZE] バイトずつ読み込んでパートをアップロードし、
     * 累計バイト数が [ImageStorageGateway.PutRequest.maxBytes] を超えた時点で
     * アップロードを中止して [ImageStorageGateway.PutResult.PayloadTooLarge] を返す。
     * ローカル一時ファイルは使用しない。
     */
    private fun putWithMultipart(request: ImageStorageGateway.PutRequest): ImageStorageGateway.PutResult {
        val key = buildKey(request.userId.value.toString(), request.relativePath)
        val s3Client = runCatching {
            val credentials = stsCredentialProvider.assumeWithWebIdentity(userId = request.userId)
            buildS3Client(credentials)
        }.getOrElse { e ->
            return ImageStorageGateway.PutResult.Failure(e)
        }

        val uploadId = runCatching {
            s3Client.createMultipartUpload(
                CreateMultipartUploadRequest.builder()
                    .bucket(config.bucket)
                    .key(key)
                    .contentType(request.contentType)
                    .build(),
            ).uploadId()
        }.getOrElse { e ->
            s3Client.close()
            return ImageStorageGateway.PutResult.Failure(e)
        }

        fun abort() {
            runCatching {
                s3Client.abortMultipartUpload(
                    AbortMultipartUploadRequest.builder()
                        .bucket(config.bucket)
                        .key(key)
                        .uploadId(uploadId)
                        .build(),
                )
            }
            s3Client.close()
        }

        val completedParts = mutableListOf<CompletedPart>()
        var partNumber = 1
        var totalBytes = 0L
        val buffer = ByteArray(MULTIPART_PART_SIZE)

        try {
            while (true) {
                // バッファが満杯になるか EOF になるまで読み込む
                var bytesRead = 0
                while (bytesRead < MULTIPART_PART_SIZE) {
                    val read = request.inputStream.read(buffer, bytesRead, MULTIPART_PART_SIZE - bytesRead)
                    if (read == -1) break
                    bytesRead += read
                }
                if (bytesRead == 0) break

                totalBytes += bytesRead
                if (totalBytes > request.maxBytes) {
                    abort()
                    return ImageStorageGateway.PutResult.PayloadTooLarge
                }

                val uploadPartResponse = s3Client.uploadPart(
                    UploadPartRequest.builder()
                        .bucket(config.bucket)
                        .key(key)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .contentLength(bytesRead.toLong())
                        .build(),
                    RequestBody.fromBytes(buffer.copyOfRange(0, bytesRead)),
                )
                completedParts.add(
                    CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(uploadPartResponse.eTag())
                        .build(),
                )
                partNumber++

                if (bytesRead < MULTIPART_PART_SIZE) break // EOF に達した
            }

            if (totalBytes == 0L) {
                abort()
                return ImageStorageGateway.PutResult.Empty
            }

            s3Client.completeMultipartUpload(
                CompleteMultipartUploadRequest.builder()
                    .bucket(config.bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(
                        CompletedMultipartUpload.builder()
                            .parts(completedParts)
                            .build(),
                    )
                    .build(),
            )
            s3Client.close()
            return ImageStorageGateway.PutResult.Success(relativePath = request.relativePath)
        } catch (e: Exception) {
            abort()
            return ImageStorageGateway.PutResult.Failure(e)
        }
    }

    override fun delete(request: ImageStorageGateway.DeleteRequest): ImageStorageGateway.DeleteResult {
        val key = buildKey(request.userId.value.toString(), request.relativePath)

        return runCatching {
            val credentials = stsCredentialProvider.assumeWithWebIdentity(userId = request.userId)
            buildS3Client(credentials).use { s3Client ->
                val deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(config.bucket)
                    .key(key)
                    .build()
                s3Client.deleteObject(deleteObjectRequest)
            }
        }.fold(
            onSuccess = { ImageStorageGateway.DeleteResult.Success },
            onFailure = { ImageStorageGateway.DeleteResult.Failure(it) },
        )
    }

    /**
     * 画像取得時にS3のpresigned URLを直接返している。
     * 呼び出しのたびにSTS APIとS3Presignerを構築するため、画像が多いと
     * STS APIのレート制限やレイテンシが問題になる可能性がある。
     * 改善策1: StsCredentialProviderでuserId単位にcredentialをキャッシュする
     * 改善策2: GraphQLがpresigned URLを直接返すよう変更し、DataLoaderで
     *          同一GraphQLリクエスト内の複数画像をuserId単位にまとめてSTS呼び出しを削減する
     *          （現状はGraphQLが /api/image/v1/{displayId} を返しブラウザが個別にHTTPリクエストするため
     *           DataLoaderは適用できない）
     */
    override fun buildDisplayUrl(request: ImageStorageGateway.BuildUrlRequest): String {
        val credentials = stsCredentialProvider.assumeWithWebIdentity(userId = request.userId)
        val key = buildKey(request.userId.value.toString(), request.relativePath)

        S3Presigner.builder().apply {
            if (config.endpoint.isNotBlank()) {
                endpointOverride(URI(config.endpoint))
            }
            region(Region.of(config.region))
            credentialsProvider(StaticCredentialsProvider.create(credentials))
            serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(config.pathStyleAccess)
                    .build(),
            )
        }.build().use { presigner ->
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(config.bucket)
                .key(key)
                .build()

            val presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getObjectRequest)
                .build()

            val presignedGetObjectResponse = presigner.presignGetObject(presignRequest)
            return presignedGetObjectResponse.url().toString()
        }
    }

    private fun buildKey(userIdStr: String, relativePath: String): String {
        return "img/$userIdStr/$relativePath"
    }
}
