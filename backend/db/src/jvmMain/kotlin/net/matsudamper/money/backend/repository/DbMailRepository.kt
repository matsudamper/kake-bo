package net.matsudamper.money.backend.repository

import java.time.LocalDateTime
import net.matsudamper.money.backend.DbConnection
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.db.schema.tables.JMoneyUsagesMailsRelation
import net.matsudamper.money.db.schema.tables.JUserMails
import net.matsudamper.money.db.schema.tables.records.JUserMailsRecord
import net.matsudamper.money.element.ImportedMailId
import org.jooq.impl.DSL
import org.jooq.kotlin.and

class DbMailRepository {
    private val userMails = JUserMails.USER_MAILS
    private val relation = JMoneyUsagesMailsRelation.MONEY_USAGES_MAILS_RELATION

    fun addMail(
        userId: UserId,
        plainText: String?,
        html: String?,
        from: String,
        subject: String,
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
                            fromMail = from,
                            subject = subject,
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

    fun getCount(userId: UserId): Int? {
        return DbConnection.use { connection ->
            val result = DSL.using(connection)
                .select(DSL.count())
                .from(userMails)
                .where(userMails.USER_ID.eq(userId.id))
                .fetchOne()

            result?.get(DSL.count())
        }
    }

    fun getMails(
        userId: UserId,
        size: Int,
        lastMailId: ImportedMailId?,
        isAsc: Boolean,
        isLinked: Boolean?,
    ): List<Mail> {
        return DbConnection.use { connection ->
            val result = DSL.using(connection)
                .select(
                    userMails.USER_MAIL_ID,
                    userMails.PLAIN,
                    userMails.HTML,
                    userMails.FROM_MAIL,
                    userMails.SUBJECT,
                    userMails.DATETIME,
                )
                .from(userMails)
                .leftJoin(relation).using(userMails.USER_MAIL_ID)
                .where(
                    DSL.value(true)
                        .and(userMails.USER_ID.eq(userId.id))
                        .and(
                            when(isLinked) {
                                true -> relation.MONEY_USAGE_ID.isNotNull
                                false -> relation.MONEY_USAGE_ID.isNull
                                null -> DSL.value(true)
                            }
                        )
                        .and(
                            if (lastMailId == null) {
                                DSL.value(true)
                            } else {
                                if (isAsc) {
                                    DSL.and(userMails.USER_MAIL_ID.greaterThan(lastMailId.id))
                                } else {
                                    DSL.and(userMails.USER_MAIL_ID.lessThan(lastMailId.id))
                                }
                            }
                        ),
                )
                .orderBy(
                    userMails.USER_MAIL_ID.run {
                        if (isAsc) {
                            asc()
                        } else {
                            desc()
                        }
                    }
                )
                .limit(size)
                .fetch()

            result.map {
                it.get(userMails.USER_MAIL_ID)
                it.get(userMails.PLAIN)
                it.get(userMails.HTML)
                it.get(userMails.FROM_MAIL)
                Mail(
                    id = ImportedMailId(it.get(userMails.USER_MAIL_ID)!!),
                    plain = it.get(userMails.PLAIN),
                    html = it.get(userMails.HTML),
                    from = it.get(userMails.FROM_MAIL)!!,
                    subject = it.get(userMails.SUBJECT)!!,
                    dateTime = it.get(userMails.DATETIME)!!,
                )
            }
        }
    }

    fun getMails(userId: UserId, mailIds: List<ImportedMailId>): List<Mail> {
        return DbConnection.use { connection ->
            val result = DSL.using(connection)
                .select(
                    userMails.USER_MAIL_ID,
                    userMails.PLAIN,
                    userMails.HTML,
                    userMails.FROM_MAIL,
                    userMails.SUBJECT,
                    userMails.DATETIME,
                )
                .from(userMails)
                .where(
                    userMails.USER_ID.eq(userId.id)
                        .and(userMails.USER_MAIL_ID.`in`(mailIds.map { it.id })),
                )
                .fetch()

            result.map {
                it.get(userMails.USER_MAIL_ID)
                it.get(userMails.PLAIN)
                it.get(userMails.HTML)
                it.get(userMails.FROM_MAIL)
                Mail(
                    id = ImportedMailId(it.get(userMails.USER_MAIL_ID)!!),
                    plain = it.get(userMails.PLAIN),
                    html = it.get(userMails.HTML),
                    from = it.get(userMails.FROM_MAIL)!!,
                    subject = it.get(userMails.SUBJECT)!!,
                    dateTime = it.get(userMails.DATETIME)!!,
                )
            }
        }
    }

    public data class Mail(
        val id: ImportedMailId,
        val plain: String?,
        val html: String?,
        val from: String,
        val subject: String,
        val dateTime: LocalDateTime,
    )

    sealed interface AddUserResult {
        object Success : AddUserResult
        data class Failed(val error: ErrorType) : AddUserResult

        sealed interface ErrorType {
            class InternalServerError(val e: Throwable) : ErrorType
        }
    }
}
