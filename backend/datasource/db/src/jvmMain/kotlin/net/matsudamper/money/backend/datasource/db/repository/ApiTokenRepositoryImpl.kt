package net.matsudamper.money.backend.datasource.db.repository

import java.time.ZoneOffset
import kotlin.jvm.optionals.getOrNull
import net.matsudamper.money.backend.app.interfaces.ApiTokenRepository
import net.matsudamper.money.backend.datasource.db.DbConnection
import net.matsudamper.money.db.schema.tables.JApiTokens
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL
import org.jooq.kotlin.and

class ApiTokenRepositoryImpl(
    private val dbConnection: DbConnection,
) : ApiTokenRepository {
    private val apiTokens = JApiTokens.API_TOKENS
    override fun registerToken(
        id: UserId,
        name: String,
        keyLength: Int,
        iterationCount: Int,
        hashedToken: String,
        algorithm: String,
        salt: ByteArray,
    ) {
        dbConnection.use { con ->
            DSL.using(con)
                .insertInto(apiTokens)
                .set(apiTokens.USER_ID, id.value)
                .set(apiTokens.KEY_LENGTH, keyLength)
                .set(apiTokens.ITERATION_COUNT, iterationCount)
                .set(apiTokens.TOKEN_HASH, hashedToken)
                .set(apiTokens.SALT, salt)
                .set(apiTokens.DISPLAY_NAME, name)
                .set(apiTokens.ALGORITHM, algorithm)
                .execute()
        }
    }

    override fun verifyToken(id: UserId, hashedToken: String): ApiTokenRepository.VerifyTokenResult {
        val result = dbConnection.use { con ->
            DSL.using(con)
                .select(
                    apiTokens.USER_ID,
                    apiTokens.PERMISSIONS,
                )
                .from(apiTokens)
                .where(
                    DSL.value(true)
                        .and(apiTokens.USER_ID.eq(id.value))
                        .and(apiTokens.TOKEN_HASH.eq(hashedToken)),
                )
                .fetchOptional()
                .getOrNull()
        } ?: throw IllegalArgumentException("Invalid token")

        return ApiTokenRepository.VerifyTokenResult(
            userId = UserId(result.get(apiTokens.USER_ID)!!),
            permissions = result.get(apiTokens.PERMISSIONS)!!,
        )
    }

    override fun getApiTokens(id: UserId): List<ApiTokenRepository.ApiToken> {
        return dbConnection.use { con ->
            DSL.using(con)
                .select(
                    apiTokens.DISPLAY_NAME,
                    apiTokens.EXPIRE_DATETIME,
                )
                .from(apiTokens)
                .where(apiTokens.USER_ID.eq(id.value))
                .fetch()
                .map {
                    ApiTokenRepository.ApiToken(
                        name = it.get(apiTokens.DISPLAY_NAME)!!,
                        expiredAt = it.get(apiTokens.EXPIRE_DATETIME)!!.toInstant(ZoneOffset.UTC),
                    )
                }
        }
    }
}
