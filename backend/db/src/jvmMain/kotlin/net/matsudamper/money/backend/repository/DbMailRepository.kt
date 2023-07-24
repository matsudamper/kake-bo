package net.matsudamper.money.backend.repository

import java.time.LocalDateTime
import net.matsudamper.money.backend.DbConnection
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.db.schema.tables.JUserMails
import net.matsudamper.money.db.schema.tables.records.JUserMailsRecord
import org.jooq.impl.DSL

class DbMailRepository {
    private val userMails = JUserMails.USER_MAILS

    fun addMail(
        userId: UserId,
        plainText: String,
        html: String,
        dateTime: LocalDateTime,
    ): AddUserResult {
        runCatching {
            DbConnection.use { connection ->
                DSL.using(connection)
                    .insertInto(userMails)
                    .set(
                        JUserMailsRecord(
                            userId = userId.id,
                            plain = plainText,
                            html = html,
                            datetime = dateTime,
                        ),
                    )
                    .execute()
            }
        }
            .onFailure { e ->
                return AddUserResult.Failed(
                    AddUserResult.ErrorType.InternalServerError(e),
                )
            }

        return AddUserResult.Success
    }

    sealed interface AddUserResult {
        object Success : AddUserResult
        data class Failed(val error: ErrorType) : AddUserResult

        sealed interface ErrorType {
            class InternalServerError(val e: Throwable) : ErrorType
        }
    }
}
