package net.matsudamper.money.backend.base

public object ServerEnv {
    public val isSecure: Boolean get() = requiredBoolean("IS_SECURE")
    public val domain: String? get() = System.getenv()["DOMAIN"]
    public val port: Int get() = required("PORT").toInt()
    public val frontPath: String get() = required("HTML_PATH")
    public val htmlPath: String get() = "$frontPath/index.html"
    public val imageStoragePath: String get() = required("IMAGE_STORAGE_PATH")
    public val imageUploadMaxBytes: Long get() = 50L * 1024L * 1024L // 50MB
    public val isDebug: Boolean get() = requiredBoolean("IS_DEBUG")

    public val dbHost: String get() = required("DB_HOST")
    public val dbPort: String get() = required("DB_PORT")
    public val dbSchema: String get() = required("DB_SCHEMA")
    public val dbUserName: String get() = required("DB_USERNAME")
    public val dbPassword: String get() = required("DB_PASSWORD")

    public val appPackageName: String get() = required("APP_PACKAGE_NAME")
    public val appFingerprint: String get() = required("APP_FINGERPRINT")
    public val apkKeyHash: String get() = required("APK_KEY_HASH")

    public val userPasswordPepper: String get() = required("USER_PASSWORD_PEPPER")

    /**
     * DBに保存する秘密情報を暗号化する鍵。32バイトの乱数をBase64にした値を指定する。
     * 未設定の場合は暗号化が必要な情報を保存できない。
     *
     * 生成方法:
     * ```
     * openssl rand -base64 32
     * ```
     */
    public val dbSecretEncryptionKey: String? get() = System.getenv("DB_SECRET_ENCRYPTION_KEY")?.takeIf { it.isNotBlank() }

    public val adminPasswordHash: String? get() = System.getenv("ADMIN_PASSWORD_HASH")
    public val adminPasswordSalt: String? get() = System.getenv("ADMIN_PASSWORD_SALT")

    /**
     * 管理画面へユーザーセッションでログインできるユーザーのID。カンマ区切りで指定する。例: `1,2`
     */
    public val adminUserIds: Set<Int>
        get() = System.getenv("ADMIN_USER_IDS").orEmpty()
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()

    public val adminPasswordAlgorithm: String get() = "PBKDF2WithHmacSHA512"
    public val adminPasswordIterationCount: Int get() = 100000
    public val adminPasswordKeyLength: Int get() = 512

    public val enableRedis: Boolean get() = requiredBoolean("ENABLE_REDIS")
    public val redisHost: String? get() = System.getenv("REDIS_HOST")?.takeIf { it.isNotBlank() }
    public val redisPort: Int? get() = System.getenv("REDIS_PORT")?.toInt()

    public val enableS3: Boolean get() = requiredBoolean("ENABLE_S3")
    public val S3: S3Env? by lazy { if (enableS3) S3Env() else null }

    public class S3Env {
        public val oidcIssuer: String get() = required("OIDC_ISSUER")
        public val oidcJwkPrivate: String get() = required("OIDC_JWK_PRIVATE")
        public val s3Endpoint: String get() = required("S3_ENDPOINT")
        public val s3Region: String? get() = System.getenv("S3_REGION")
        public val s3Bucket: String get() = required("S3_BUCKET")
        public val s3RoleArn: String get() = required("S3_ROLE_ARN")
        public val s3RoleSessionName: String get() = required("S3_ROLE_SESSION_NAME")
        public val s3Audience: String get() = required("S3_AUDIENCE")
        public val s3PathStyleAccess: Boolean get() = requiredBoolean("S3_PATH_STYLE_ACCESS")
        public val stsEndpoint: String get() = required("STS_ENDPOINT")
        public val imageResizerEndpoint: String get() = required("IMAGE_RESIZER_ENDPOINT")
    }

    private fun required(name: String): String {
        return requireNotNull(System.getenv(name)?.takeIf { it.isNotBlank() }) { "$name が未設定です" }
    }

    private fun requiredBoolean(name: String): Boolean {
        val raw = required(name)
        return requireNotNull(raw.lowercase().toBooleanStrictOrNull()) { "$name は true か false にすること: $raw" }
    }
}
