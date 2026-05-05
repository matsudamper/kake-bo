package net.matsudamper.money.backend.di

import java.time.Clock
import net.matsudamper.money.backend.app.interfaces.AdminImageRepository
import net.matsudamper.money.backend.app.interfaces.AdminLoginRepository
import net.matsudamper.money.backend.app.interfaces.AdminRepository
import net.matsudamper.money.backend.app.interfaces.AdminSessionRepository
import net.matsudamper.money.backend.app.interfaces.ApiTokenRepository
import net.matsudamper.money.backend.app.interfaces.ChallengeRepository
import net.matsudamper.money.backend.app.interfaces.DeleteUsageImageRelationDao
import net.matsudamper.money.backend.app.interfaces.FidoRepository
import net.matsudamper.money.backend.app.interfaces.ImageStorageGateway
import net.matsudamper.money.backend.app.interfaces.ImportedMailRepository
import net.matsudamper.money.backend.app.interfaces.MailFilterRepository
import net.matsudamper.money.backend.app.interfaces.MailRepository
import net.matsudamper.money.backend.app.interfaces.MoneyUsageAnalyticsRepository
import net.matsudamper.money.backend.app.interfaces.MoneyUsageCategoryRepository
import net.matsudamper.money.backend.app.interfaces.MoneyUsagePresetRepository
import net.matsudamper.money.backend.app.interfaces.MoneyUsageRepository
import net.matsudamper.money.backend.app.interfaces.MoneyUsageSubCategoryRepository
import net.matsudamper.money.backend.app.interfaces.UserConfigRepository
import net.matsudamper.money.backend.app.interfaces.UserImageRepository
import net.matsudamper.money.backend.app.interfaces.UserLoginRepository
import net.matsudamper.money.backend.app.interfaces.UserRepository
import net.matsudamper.money.backend.app.interfaces.UserSessionRepository
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.base.ServerVariables
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.backend.datasource.challenge.ChallengeRepositoryProvider
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.backend.datasource.db.repository.AdminRepositoryImpl
import net.matsudamper.money.backend.datasource.db.repository.ApiTokenRepositoryImpl
import net.matsudamper.money.backend.datasource.db.repository.DbAdminImageRepository
import net.matsudamper.money.backend.datasource.db.repository.DbAdminSessionRepository
import net.matsudamper.money.backend.datasource.db.repository.DbFidoRepository
import net.matsudamper.money.backend.datasource.db.repository.DbImportedImportedMailRepository
import net.matsudamper.money.backend.datasource.db.repository.DbMailFilterRepository
import net.matsudamper.money.backend.datasource.db.repository.DbMoneyUsageAnalyticsRepository
import net.matsudamper.money.backend.datasource.db.repository.DbMoneyUsageCategoryRepository
import net.matsudamper.money.backend.datasource.db.repository.DbMoneyUsagePresetRepository
import net.matsudamper.money.backend.datasource.db.repository.DbMoneyUsageRepository
import net.matsudamper.money.backend.datasource.db.repository.DbMoneyUsageSubCategoryRepository
import net.matsudamper.money.backend.datasource.db.repository.DbUserConfigRepository
import net.matsudamper.money.backend.datasource.db.repository.DbUserImageRepository
import net.matsudamper.money.backend.datasource.db.repository.DbUserLoginRepository
import net.matsudamper.money.backend.datasource.db.repository.DbUserRepository
import net.matsudamper.money.backend.datasource.db.repository.DeleteUsageImageRelationDaoImpl
import net.matsudamper.money.backend.datasource.db.repository.EnvAdminLoginRepository
import net.matsudamper.money.backend.datasource.session.AdminSessionRepositoryProvider
import net.matsudamper.money.backend.datasource.session.UserSessionRepositoryProvider
import net.matsudamper.money.backend.feature.imagestoragelocal.LocalImageStorageGateway
import net.matsudamper.money.backend.feature.objectstorage.ObjectStorageConfig
import net.matsudamper.money.backend.feature.objectstorage.S3ImageStorageGateway
import net.matsudamper.money.backend.feature.objectstorage.StsCredentialProvider
import net.matsudamper.money.backend.feature.oidc.JwtIssuer
import net.matsudamper.money.backend.feature.oidc.OidcKeyManager
import net.matsudamper.money.backend.mail.MailRepositoryImpl

interface DiContainer {
    fun createMailRepository(
        host: String,
        port: Int,
        userName: String,
        password: String,
    ): MailRepository

    fun createUserSessionRepository(): UserSessionRepository

    fun createUserConfigRepository(): UserConfigRepository

    fun createDbMailRepository(): ImportedMailRepository

    fun createMoneyUsageCategoryRepository(): MoneyUsageCategoryRepository

    fun createMoneyUsageSubCategoryRepository(): MoneyUsageSubCategoryRepository

    fun createMoneyUsagePresetRepository(): MoneyUsagePresetRepository

    fun createMoneyUsageRepository(): MoneyUsageRepository

    fun createMailFilterRepository(): MailFilterRepository

    fun createMoneyUsageAnalyticsRepository(): MoneyUsageAnalyticsRepository

    fun createUserNameRepository(): UserRepository

    fun createUserImageRepository(): UserImageRepository

    fun createFidoRepository(): FidoRepository

    fun createChallengeRepository(): ChallengeRepository

    fun createDeleteUsageImageRelationDao(): DeleteUsageImageRelationDao

    fun createAdminUserSessionRepository(): AdminSessionRepository

    fun createAdminRepository(): AdminRepository

    fun createAdminLoginRepository(): AdminLoginRepository

    fun createAdminImageRepository(): AdminImageRepository

    fun userLoginRepository(): UserLoginRepository
    fun createApiTokenRepository(): ApiTokenRepository
    fun traceLogger(): TraceLogger
    fun clock(): Clock

    fun createOidcKeyManager(): OidcKeyManager?
    fun createJwtIssuer(): JwtIssuer?
    fun createWriteImageStorageGateway(): ImageStorageGateway
    fun createReadImageStorageGateway(storageType: UserImageRepository.StorageType): ImageStorageGateway
}

class MainDiContainer : DiContainer {
    override fun createAdminRepository(): AdminRepository {
        return AdminRepositoryImpl()
    }

    override fun createAdminLoginRepository(): AdminLoginRepository {
        return EnvAdminLoginRepository()
    }

    private val adminImageRepository by lazy {
        DbAdminImageRepository(
            localImageStorageGateway = localImageStorageGateway,
            s3ImageStorageGateway = s3ImageStorageGateway,
        )
    }

    override fun createAdminImageRepository(): AdminImageRepository {
        return adminImageRepository
    }

    override fun createMailRepository(
        host: String,
        port: Int,
        userName: String,
        password: String,
    ): MailRepository {
        return MailRepositoryImpl(
            host = host,
            port = port,
            userName = userName,
            password = password,
        )
    }

    private val userSessionRepository: UserSessionRepository = if (ServerEnv.enableRedis) {
        UserSessionRepositoryProvider.provideRedisRepository(
            host = ServerEnv.redisHost!!,
            port = ServerEnv.redisPort!!,
            index = ServerVariables.REDIS_INDEX_USER_SESSION,
            clock = clock(),
        )
    } else {
        UserSessionRepositoryProvider.provideLocalRepository(clock = clock())
    }

    override fun createUserSessionRepository(): UserSessionRepository {
        return userSessionRepository
    }

    private val mailFilterRepository = DbMailFilterRepository(dbConnection = DbConnectionImpl)

    override fun createMailFilterRepository(): MailFilterRepository {
        return mailFilterRepository
    }

    private val userConfigRepository = DbUserConfigRepository()

    override fun createUserConfigRepository(): DbUserConfigRepository {
        return userConfigRepository
    }

    private val dbImportedMailRepository = DbImportedImportedMailRepository(dbConnection = DbConnectionImpl)

    override fun createDbMailRepository(): DbImportedImportedMailRepository {
        return dbImportedMailRepository
    }

    private val moneyUsageCategoryRepository = DbMoneyUsageCategoryRepository()

    override fun createMoneyUsageCategoryRepository(): MoneyUsageCategoryRepository {
        return moneyUsageCategoryRepository
    }

    private val moneyUsagePresetRepository = DbMoneyUsagePresetRepository()

    override fun createMoneyUsagePresetRepository(): MoneyUsagePresetRepository {
        return moneyUsagePresetRepository
    }

    private val moneyUsageSubCategoryRepository = DbMoneyUsageSubCategoryRepository()

    override fun createMoneyUsageSubCategoryRepository(): MoneyUsageSubCategoryRepository {
        return moneyUsageSubCategoryRepository
    }

    private val moneyUsageRepository = DbMoneyUsageRepository()

    override fun createMoneyUsageRepository(): MoneyUsageRepository {
        return moneyUsageRepository
    }

    private val dbMoneyUsageAnalyticsRepository = DbMoneyUsageAnalyticsRepository(dbConnection = DbConnectionImpl)

    override fun createMoneyUsageAnalyticsRepository(): DbMoneyUsageAnalyticsRepository {
        return dbMoneyUsageAnalyticsRepository
    }

    private val userRepository = DbUserRepository()

    override fun createUserNameRepository(): UserRepository {
        return userRepository
    }

    private val userImageRepository = DbUserImageRepository()

    override fun createUserImageRepository(): UserImageRepository {
        return userImageRepository
    }

    private val deleteUsageImageRelationDao = DeleteUsageImageRelationDaoImpl()

    override fun createDeleteUsageImageRelationDao(): DeleteUsageImageRelationDao {
        return deleteUsageImageRelationDao
    }

    private val fidoRepository = DbFidoRepository(dbConnection = DbConnectionImpl)

    override fun createFidoRepository(): FidoRepository {
        return fidoRepository
    }

    private val challengeRepository: ChallengeRepository = if (ServerEnv.enableRedis) {
        ChallengeRepositoryProvider.provideRedisRepository(
            host = ServerEnv.redisHost!!,
            port = ServerEnv.redisPort!!,
            index = ServerVariables.REDIS_INDEX_CHALLENGE,
        )
    } else {
        ChallengeRepositoryProvider.provideLocalRepository(clock = clock())
    }

    override fun createChallengeRepository(): ChallengeRepository {
        return challengeRepository
    }

    private val adminSessionRepository: AdminSessionRepository = if (ServerEnv.enableRedis) {
        AdminSessionRepositoryProvider.provideRedisRepository(
            host = ServerEnv.redisHost!!,
            port = ServerEnv.redisPort!!,
            index = ServerVariables.REDIS_INDEX_ADMIN_SESSION,
            clock = clock(),
        )
    } else {
        DbAdminSessionRepository(dbConnection = DbConnectionImpl, clock = clock())
    }

    override fun createAdminUserSessionRepository(): AdminSessionRepository {
        return adminSessionRepository
    }

    override fun userLoginRepository(): UserLoginRepository {
        return DbUserLoginRepository(dbConnection = DbConnectionImpl)
    }

    override fun createApiTokenRepository(): ApiTokenRepository {
        return ApiTokenRepositoryImpl(dbConnection = DbConnectionImpl)
    }

    override fun traceLogger(): TraceLogger {
        return TraceLogger.impl()
    }

    override fun clock(): Clock {
        return Clock.systemUTC()
    }

    private val oidcKeyManager by lazy {
        if (ServerEnv.enableS3) {
            val jwkPrivate = ServerEnv.oidcJwkPrivate
            require(!jwkPrivate.isNullOrBlank()) { "OIDC_JWK_PRIVATE が未設定です" }
            OidcKeyManager(jwkPrivate)
        } else {
            null
        }
    }

    override fun createOidcKeyManager(): OidcKeyManager? {
        return oidcKeyManager
    }

    private val jwtIssuer by lazy {
        val keyManager = createOidcKeyManager()
        if (keyManager != null) {
            val issuer = ServerEnv.oidcIssuer
            require(!issuer.isNullOrBlank()) { "OIDC_ISSUER が未設定です" }
            JwtIssuer(keyManager, issuer)
        } else {
            null
        }
    }

    override fun createJwtIssuer(): JwtIssuer? {
        return jwtIssuer
    }

    private val objectStorageConfig by lazy {
        if (ServerEnv.enableS3) {
            val region = ServerEnv.s3Region
            val bucket = ServerEnv.s3Bucket
            val roleArn = ServerEnv.s3RoleArn
            val audience = ServerEnv.s3Audience
            require(!region.isNullOrBlank()) { "S3_REGION が未設定です" }
            require(!bucket.isNullOrBlank()) { "S3_BUCKET が未設定です" }
            require(!roleArn.isNullOrBlank()) { "S3_ROLE_ARN が未設定です" }
            require(!audience.isNullOrBlank()) { "S3_AUDIENCE が未設定です" }
            ObjectStorageConfig(
                endpoint = ServerEnv.s3Endpoint.orEmpty(),
                stsEndpoint = ServerEnv.stsEndpoint.orEmpty(),
                region = region,
                bucket = bucket,
                roleArn = roleArn,
                roleSessionName = ServerEnv.s3RoleSessionName,
                audience = audience,
                pathStyleAccess = ServerEnv.s3PathStyleAccess,
            )
        } else {
            null
        }
    }

    private val stsCredentialProvider by lazy {
        val issuer = createJwtIssuer()
        val config = objectStorageConfig
        if (issuer != null && config != null) {
            StsCredentialProvider(
                jwtIssuer = issuer,
                config = config,
            )
        } else {
            null
        }
    }

    private val s3ImageStorageGateway by lazy {
        val provider = stsCredentialProvider
        val config = objectStorageConfig
        if (provider != null && config != null) {
            S3ImageStorageGateway(
                stsCredentialProvider = provider,
                config = config,
            )
        } else {
            null
        }
    }

    private val localImageStorageGateway by lazy {
        LocalImageStorageGateway(
            storageDirectory = java.io.File(ServerEnv.imageStoragePath),
        )
    }

    override fun createWriteImageStorageGateway(): ImageStorageGateway {
        return if (ServerEnv.enableS3) {
            s3ImageStorageGateway ?: throw IllegalStateException("S3 is enabled but credentials/config are missing.")
        } else {
            localImageStorageGateway
        }
    }

    override fun createReadImageStorageGateway(
        storageType: UserImageRepository.StorageType,
    ): ImageStorageGateway {
        return when (storageType) {
            UserImageRepository.StorageType.LOCAL -> localImageStorageGateway
            UserImageRepository.StorageType.S3 ->
                s3ImageStorageGateway
                    ?: throw IllegalStateException("Cannot read S3 image without S3 config.")
        }
    }

    init {
        // S3 関連の必須設定を起動時に検証して fail-fast にする。
        // 各 lazy が non-blank/non-null を require しているため、ここで強制評価する。
        if (ServerEnv.enableS3) {
            objectStorageConfig
            oidcKeyManager
            jwtIssuer
            stsCredentialProvider
            s3ImageStorageGateway
        }
    }
}
