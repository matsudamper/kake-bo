package net.matsudamper.money.backend.base

public object ServerEnv {
    public val isSecure: Boolean get() = System.getenv()["IS_SECURE"].toBoolean()
    public val domain: String? get() = System.getenv()["DOMAIN"]
    public val port: Int get() = System.getenv()["PORT"]!!.toInt()
    public val frontPath: String get() = System.getenv()["HTML_PATH"] ?: "../frontend/app/build/dist/js/developmentExecutable"
    public val htmlPath: String get() = "$frontPath/index.html"
    public val imageStoragePath: String get() = System.getenv()["IMAGE_STORAGE_PATH"] ?: "./uploaded_images"
    public val imageUploadMaxBytes: Long get() = 50L * 1024L * 1024L // 50MB
    public val isDebug: Boolean get() = System.getenv()["IS_DEBUG"]?.toBooleanStrictOrNull() ?: false

    public val dbHost: String get() = System.getenv()["DB_HOST"]!!
    public val dbPort: String get() = System.getenv()["DB_PORT"]!!
    public val dbSchema: String get() = System.getenv()["DB_SCHEMA"]!!
    public val dbUserName: String get() = System.getenv()["DB_USERNAME"]!!
    public val dbPassword: String get() = System.getenv()["DB_PASSWORD"]!!

    public val appPackageName: String get() = System.getenv()["APP_PACKAGE_NAME"]!!
    public val appFingerprint: String get() = System.getenv()["APP_FINGERPRINT"]!!
    public val apkKeyHash: String get() = System.getenv()["APK_KEY_HASH"].orEmpty()

    public val userPasswordPepper: String get() = System.getenv("USER_PASSWORD_PEPPER")!!

    public val adminPasswordHash: String? get() = System.getenv("ADMIN_PASSWORD_HASH")
    public val adminPasswordSalt: String? get() = System.getenv("ADMIN_PASSWORD_SALT")

    public val adminPasswordAlgorithm: String get() = "PBKDF2WithHmacSHA512"
    public val adminPasswordIterationCount: Int get() = 100000
    public val adminPasswordKeyLength: Int get() = 512

    public val enableRedis: Boolean get() = System.getenv("ENABLE_REDIS")?.toBooleanStrictOrNull() ?: false
    public val redisHost: String? get() = System.getenv("REDIS_HOST")?.takeIf { it.isNotBlank() }
    public val redisPort: Int? get() = System.getenv("REDIS_PORT")?.toInt()

    public val enableS3: Boolean get() = System.getenv("ENABLE_S3")?.toBooleanStrictOrNull() ?: false
    public val oidcIssuer: String? get() = System.getenv("OIDC_ISSUER")
    public val oidcJwkPrivate: String? get() = System.getenv("OIDC_JWK_PRIVATE")
    public val s3Endpoint: String? get() = System.getenv("S3_ENDPOINT")
    public val s3Region: String? get() = System.getenv("S3_REGION")
    public val s3Bucket: String? get() = System.getenv("S3_BUCKET")
    public val s3RoleArn: String? get() = System.getenv("S3_ROLE_ARN")
    public val s3RoleSessionName: String get() = System.getenv("S3_ROLE_SESSION_NAME") ?: "kake-bo-image-upload"
    public val s3Audience: String? get() = System.getenv("S3_AUDIENCE")
    public val s3PathStyleAccess: Boolean get() = System.getenv("S3_PATH_STYLE_ACCESS")?.toBooleanStrictOrNull() ?: true
    public val stsEndpoint: String? get() = System.getenv("STS_ENDPOINT")
}
