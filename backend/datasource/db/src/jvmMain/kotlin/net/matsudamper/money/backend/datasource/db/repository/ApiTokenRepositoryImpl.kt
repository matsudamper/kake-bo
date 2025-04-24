package net.matsudamper.money.backend.datasource.db.repository

import java.time.ZoneOffset
import java.util.Base64
import kotlin.jvm.optionals.getOrNull
import net.matsudamper.money.backend.app.interfaces.ApiTokenRepository
import net.matsudamper.money.backend.datasource.db.DbConnection
import net.matsudamper.money.db.schema.tables.JApiTokens
import net.matsudamper.money.element.ApiTokenId
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL
import org.jooq.kotlin.and

class ApiTokenRepositoryImpl(
    private val dbConnection: DbConnection,
) : ApiTokenRepository {
    private val apiTokens = JApiTokens.API_TOKENS
    private val bases64Encoder = Base64.getEncoder()
    override fun registerToken(
        id: UserId,
        name: String,
        hashedToken: String,
    ) {
        dbConnection.use { con ->
            DSL.using(con)
                .insertInto(apiTokens)
                .set(apiTokens.USER_ID, id.value)
                .set(apiTokens.TOKEN_HASH, hashedToken)
                .set(apiTokens.DISPLAY_NAME, name)
                .set(apiTokens.PERMISSIONS, "")
                .execute()
        }
    }

    override fun verifyToken(hashedToken: ByteArray): ApiTokenRepository.VerifyTokenResult? {
        val encodedHashedToken = bases64Encoder.encodeToString(hashedToken)
        val result = dbConnection.use { con ->
            DSL.using(con)
                .select(
                    apiTokens.USER_ID,
                    apiTokens.PERMISSIONS,
                )
                .from(apiTokens)
                .where(
                    DSL.value(true)
                        .and(apiTokens.TOKEN_HASH.eq(encodedHashedToken)),
                )
                .fetchOptional()
                .getOrNull()
        } ?: return null

        return ApiTokenRepository.VerifyTokenResult(
            userId = UserId(result.get(apiTokens.USER_ID)!!),
            permissions = result.get(apiTokens.PERMISSIONS)!!,
        )
    }

    override fun getApiTokens(id: UserId): List<ApiTokenRepository.ApiToken> {
        return dbConnection.use { con ->
            DSL.using(con)
                .select(
                    apiTokens.API_TOKEN_ID,
                    apiTokens.DISPLAY_NAME,
                    apiTokens.EXPIRE_DATETIME,
                )
                .from(apiTokens)
                .where(apiTokens.USER_ID.eq(id.value))
                .fetch()
                .map {
                    ApiTokenRepository.ApiToken(
                        id = ApiTokenId(it.get(apiTokens.API_TOKEN_ID)!!.toString()),
                        name = it.get(apiTokens.DISPLAY_NAME)!!,
                        expiredAt = it.get(apiTokens.EXPIRE_DATETIME)?.toInstant(ZoneOffset.UTC),
                    )
                }
        }
    }
}
