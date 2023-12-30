package net.matsudamper.money.backend.repository

import net.matsudamper.money.backend.DbConnection
import net.matsudamper.money.db.schema.tables.JWebAuthAuthenticator
import net.matsudamper.money.element.FidoId
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL

class FidoRepository(
    private val dbConnection: DbConnection,
) {
    private val webAuthAuthenticator = JWebAuthAuthenticator.WEB_AUTH_AUTHENTICATOR
    fun addFido(
        name: String,
        userId: UserId,
        attestationStatement: String,
        attestationStatementFormat: String,
        attestedCredentialData: String,
        counter: Long,
    ) {
        dbConnection.use {
            DSL.using(it)
                .insertInto(webAuthAuthenticator)
                .set(webAuthAuthenticator.NAME, name)
                .set(webAuthAuthenticator.USER_ID, userId.value)
                .set(webAuthAuthenticator.ATTESTATION_STATEMENT, attestationStatement)
                .set(webAuthAuthenticator.ATTESTATION_STATEMENT_FORMAT, attestationStatementFormat)
                .set(webAuthAuthenticator.ATTESTED_CREDENTIAL_DATA, attestedCredentialData)
                .set(webAuthAuthenticator.COUNTER, counter)
                .execute()
        }
    }

    fun getFidoNames(
        userId: UserId,
    ): List<FidoNameResult> {
        return dbConnection.use {
            val results = DSL.using(it)
                .select(webAuthAuthenticator.NAME, webAuthAuthenticator.ID)
                .from(webAuthAuthenticator)
                .where(webAuthAuthenticator.USER_ID.eq(userId.value))
                .fetch()

            results.map { result ->
                FidoNameResult(
                    fidoId = FidoId(result.get<Int>(webAuthAuthenticator.ID)),
                    name = result.get<String>(webAuthAuthenticator.NAME),
                )
            }
        }
    }

    data class FidoNameResult(
        val fidoId: FidoId,
        val name: String,
    )
}
