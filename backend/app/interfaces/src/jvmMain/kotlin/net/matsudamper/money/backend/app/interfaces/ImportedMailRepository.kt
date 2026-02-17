package net.matsudamper.money.backend.app.interfaces

import java.time.LocalDateTime
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.UserId

interface ImportedMailRepository {
    fun getCount(
        userId: UserId,
        isLinked: Boolean?,
        text: String?,
    ): Int?

    fun getMail(
        userId: UserId,
        mailIds: List<ImportedMailId>,
    ): Result<MutableList<ImportedMailId>>

    fun getMails(
        userId: UserId,
        size: Int,
        pagingInfo: PagingInfo?,
        isAsc: Boolean,
        sortedKey: MailSortedKey,
        isLinked: Boolean?,
        text: String?,
    ): MailPagingResult

    fun getMails(
        userId: UserId,
        mailIds: List<ImportedMailId>,
    ): List<Mail>

    fun getMails(
        userId: UserId,
        moneyUsageIdList: List<MoneyUsageId>,
    ): Result<Map<MoneyUsageId, List<ImportedMailId>>>

    fun deleteMail(
        userId: UserId,
        mailId: ImportedMailId,
    ): Boolean

    data class Mail(
        val id: ImportedMailId,
        val plain: String?,
        val html: String?,
        val from: String,
        val subject: String,
        val dateTime: LocalDateTime,
    )

    enum class MailSortedKey {
        CREATE_DATETIME,
        DATETIME,
    }

    data class MailPagingResult(
        val mails: List<ImportedMailId>,
        val pagingInfo: PagingInfo?,
    )

    sealed interface PagingInfo {
        data class CreatedDateTime(
            val importedMailId: ImportedMailId,
            val time: LocalDateTime,
        ) : PagingInfo

        data class DateTime(
            val importedMailId: ImportedMailId,
            val time: LocalDateTime,
        ) : PagingInfo
    }

    sealed interface AddUserResult {
        data object Success : AddUserResult

        data class Failed(val error: ErrorType) : AddUserResult

        sealed interface ErrorType {
            class InternalServerError(val e: Throwable) : ErrorType
        }
    }

    fun addMail(
        userId: UserId,
        plainText: String?,
        html: String?,
        from: String,
        subject: String,
        dateTime: LocalDateTime,
    ): AddUserResult
}
