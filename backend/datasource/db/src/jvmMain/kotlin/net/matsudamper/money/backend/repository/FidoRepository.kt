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
        authenticatorExtensions: String?,
    ): RegisterdFido {
        return dbConnection.use {
            val result = DSL.using(it)
                .insertInto(webAuthAuthenticator)
                .set(webAuthAuthenticator.NAME, name)
                .set(webAuthAuthenticator.USER_ID, userId.value)
                .set(webAuthAuthenticator.ATTESTATION_STATEMENT, attestationStatement)
                .set(webAuthAuthenticator.ATTESTATION_STATEMENT_FORMAT, attestationStatementFormat)
                .set(webAuthAuthenticator.ATTESTED_CREDENTIAL_DATA, attestedCredentialData)
                .set(webAuthAuthenticator.COUNTER, counter)
                .set(webAuthAuthenticator.AUTHENTICATOR_EXTENSIONS, authenticatorExtensions)
                .returning(webAuthAuthenticator.ID, webAuthAuthenticator.NAME)
                .fetchOne()!!

            RegisterdFido(
                fidoId = FidoId(result.get<Int>(webAuthAuthenticator.ID)),
                name = name,
                attestedCredentialData = attestedCredentialData,
                counter = counter,
                attestedStatement = attestationStatement,
                attestedStatementFormat = attestationStatementFormat,
            )
        }
    }

    fun getFidoList(
        userId: UserId,
    ): List<RegisterdFido> {
        return dbConnection.use {
            val results = DSL.using(it)
                .select(
                    webAuthAuthenticator.NAME,
                    webAuthAuthenticator.ID,
                    webAuthAuthenticator.ATTESTED_CREDENTIAL_DATA,
                    webAuthAuthenticator.ATTESTATION_STATEMENT,
                    webAuthAuthenticator.ATTESTATION_STATEMENT_FORMAT,
                    webAuthAuthenticator.COUNTER,
                )
                .from(webAuthAuthenticator)
                .where(webAuthAuthenticator.USER_ID.eq(userId.value))
                .fetch()

            results.map { result ->
                RegisterdFido(
                    fidoId = FidoId(result.get<Int>(webAuthAuthenticator.ID)),
                    name = result.get<String>(webAuthAuthenticator.NAME),
                    attestedCredentialData = result.get<String>(webAuthAuthenticator.ATTESTED_CREDENTIAL_DATA),
                    counter = result.get<Long>(webAuthAuthenticator.COUNTER),
                    attestedStatement = result.get<String>(webAuthAuthenticator.ATTESTATION_STATEMENT),
                    attestedStatementFormat = result.get<String>(webAuthAuthenticator.ATTESTATION_STATEMENT_FORMAT),
                )
            }
        }
    }

    /**
     * @return isSuccess
     */
    fun deleteFido(userId: UserId, id: FidoId): Boolean {
        return dbConnection.use {
            val result = DSL.using(it)
                .deleteFrom(webAuthAuthenticator)
                .where(webAuthAuthenticator.USER_ID.eq(userId.value))
                .and(webAuthAuthenticator.ID.eq(id.value))
                .execute()

            result == 1
        }
    }

    fun updateCounter(fidoId: FidoId, userId: UserId, counter: Long) {
        dbConnection.use {
            DSL.using(it)
                .update(webAuthAuthenticator)
                .set(webAuthAuthenticator.COUNTER, counter)
                .where(
                    webAuthAuthenticator.USER_ID.eq(userId.value)
                        .and(webAuthAuthenticator.ID.eq(fidoId.value)),
                )
                .execute()
        }
    }

    data class RegisterdFido(
        val fidoId: FidoId,
        val name: String,
        val attestedCredentialData: String,
        val attestedStatement: String,
        val counter: Long,
        val attestedStatementFormat: String,
    )
}
