package net.matsudamper.money.backend.datasource.db.repository

import java.time.LocalDateTime
import net.matsudamper.money.backend.app.interfaces.ImportedMailRepository
import net.matsudamper.money.backend.datasource.db.DbConnection
import net.matsudamper.money.db.schema.tables.JMoneyUsages
import net.matsudamper.money.db.schema.tables.JMoneyUsagesMailsRelation
import net.matsudamper.money.db.schema.tables.JUserMails
import net.matsudamper.money.db.schema.tables.records.JUserMailsRecord
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL
import org.jooq.kotlin.and

class DbImportedImportedMailRepository(
    private val dbConnection: DbConnection,
) : ImportedMailRepository {
    private val userMails = JUserMails.USER_MAILS
    private val relation = JMoneyUsagesMailsRelation.MONEY_USAGES_MAILS_RELATION
    private val moneyUsages = JMoneyUsages.MONEY_USAGES

    override fun addMail(
        userId: UserId,
        plainText: String?,
        html: String?,
        from: String,
        subject: String,
        dateTime: LocalDateTime,
    ): ImportedMailRepository.AddUserResult {
        runCatching {
            dbConnection.use { connection ->
                DSL.using(connection)
                    .insertInto(userMails)
                    .set(
                        JUserMailsRecord(
                            userId = userId.value,
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
                return ImportedMailRepository.AddUserResult.Failed(
                    ImportedMailRepository.AddUserResult.ErrorType.InternalServerError(e),
                )
            }

        return ImportedMailRepository.AddUserResult.Success
    }

    override fun getCount(
        userId: UserId,
        isLinked: Boolean?,
    ): Int? {
        return dbConnection.use { connection ->
            val result = DSL.using(connection)
                .select(DSL.count())
                .from(userMails)
                .leftJoin(relation).on(
                    relation.USER_MAIL_ID.eq(userMails.USER_MAIL_ID),
                )
                .where(
                    DSL.value(true)
                        .and(userMails.USER_ID.eq(userId.value))
                        .and(
                            when (isLinked) {
                                true -> relation.MONEY_USAGE_ID.isNotNull
                                false -> relation.MONEY_USAGE_ID.isNull
                                null -> DSL.value(true)
                            },
                        ),
                )
                .fetchOne()

            result?.get(DSL.count())
        }
    }

    override fun getMail(
        userId: UserId,
        mailIds: List<ImportedMailId>,
    ): Result<MutableList<ImportedMailId>> {
        return runCatching {
            dbConnection.use { connection ->
                val results = DSL.using(connection)
                    .select(userMails.USER_MAIL_ID)
                    .from(userMails)
                    .where(
                        DSL.value(true)
                            .and(userMails.USER_ID.eq(userId.value))
                            .and(userMails.USER_MAIL_ID.`in`(mailIds.map { it.id })),
                    )
                    .fetch()

                results.map { result ->
                    ImportedMailId(result.get(userMails.USER_MAIL_ID)!!)
                }
            }
        }
    }

    override fun getMails(
        userId: UserId,
        size: Int,
        pagingInfo: ImportedMailRepository.PagingInfo?,
        isAsc: Boolean,
        sortedKey: ImportedMailRepository.MailSortedKey,
        isLinked: Boolean?,
    ): ImportedMailRepository.MailPagingResult {
        return dbConnection.use { connection ->
            val result = DSL.using(connection)
                .select(
                    userMails.USER_MAIL_ID,
                    userMails.DATETIME,
                    userMails.CREATED_DATETIME,
                )
                .from(userMails)
                .leftJoin(relation).using(relation.USER_MAIL_ID)
                .leftJoin(moneyUsages).using(moneyUsages.MONEY_USAGE_ID)
                .where(
                    DSL.value(true)
                        .and(userMails.USER_ID.eq(userId.value))
                        .and(
                            when (isLinked) {
                                true -> {
                                    relation.MONEY_USAGE_ID.isNotNull
                                }

                                false -> {
                                    moneyUsages.MONEY_USAGE_ID.isNull
                                        .or(relation.MONEY_USAGE_ID.isNull)
                                }

                                null -> DSL.value(true)
                            },
                        )
                        .and(
                            if (pagingInfo == null) {
                                DSL.value(true)
                            } else {
                                when (pagingInfo) {
                                    is ImportedMailRepository.PagingInfo.CreatedDateTime -> {
                                        val dbRow = DSL.row(userMails.CREATED_DATETIME, userMails.USER_MAIL_ID)
                                        val inputRow = DSL.row(pagingInfo.time, pagingInfo.importedMailId.id)

                                        if (isAsc) {
                                            dbRow.greaterThan(inputRow)
                                        } else {
                                            dbRow.lessThan(inputRow)
                                        }
                                    }

                                    is ImportedMailRepository.PagingInfo.DateTime -> {
                                        val dbRow = DSL.row(userMails.DATETIME, userMails.USER_MAIL_ID)
                                        val inputRow = DSL.row(pagingInfo.time, pagingInfo.importedMailId.id)

                                        if (isAsc) {
                                            dbRow.greaterThan(inputRow)
                                        } else {
                                            dbRow.lessThan(inputRow)
                                        }
                                    }
                                }
                            },
                        ),
                )
                .orderBy(
                    when (sortedKey) {
                        ImportedMailRepository.MailSortedKey.CREATE_DATETIME -> userMails.CREATED_DATETIME
                        ImportedMailRepository.MailSortedKey.DATETIME -> userMails.DATETIME
                    }.run {
                        when (isAsc) {
                            true -> asc()
                            false -> desc()
                        }
                    },
                    userMails.USER_MAIL_ID.run {
                        when (isAsc) {
                            true -> asc()
                            false -> desc()
                        }
                    },
                )
                .limit(size)
                .fetch()

            val mails = result.map {
                ImportedMailId(it.get(userMails.USER_MAIL_ID)!!)
            }
            ImportedMailRepository.MailPagingResult(
                mails = mails,
                pagingInfo = run pagingInfo@{
                    when (sortedKey) {
                        ImportedMailRepository.MailSortedKey.CREATE_DATETIME -> {
                            ImportedMailRepository.PagingInfo.CreatedDateTime(
                                importedMailId = mails.lastOrNull() ?: return@pagingInfo null,
                                time = result.lastOrNull()?.get(userMails.CREATED_DATETIME) ?: return@pagingInfo null,
                            )
                        }

                        ImportedMailRepository.MailSortedKey.DATETIME -> {
                            ImportedMailRepository.PagingInfo.DateTime(
                                importedMailId = mails.lastOrNull() ?: return@pagingInfo null,
                                time = result.lastOrNull()?.get(userMails.DATETIME) ?: return@pagingInfo null,
                            )
                        }
                    }
                },
            )
        }
    }

    override fun getMails(
        userId: UserId,
        mailIds: List<ImportedMailId>,
    ): List<ImportedMailRepository.Mail> {
        return dbConnection.use { connection ->
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
                    userMails.USER_ID.eq(userId.value)
                        .and(userMails.USER_MAIL_ID.`in`(mailIds.map { it.id })),
                )
                .fetch()

            result.map {
                it.get(userMails.USER_MAIL_ID)
                it.get(userMails.PLAIN)
                it.get(userMails.HTML)
                it.get(userMails.FROM_MAIL)
                ImportedMailRepository.Mail(
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

    override fun getMails(
        userId: UserId,
        moneyUsageIdList: List<MoneyUsageId>,
    ): Result<Map<MoneyUsageId, List<ImportedMailId>>> {
        return runCatching {
            dbConnection.use { connection ->
                val result = DSL.using(connection)
                    .select(relation.MONEY_USAGE_ID, userMails.USER_MAIL_ID)
                    .from(userMails)
                    .leftJoin(relation).using(relation.USER_MAIL_ID)
                    .where(
                        DSL.value(true)
                            .and(userMails.USER_ID.eq(userId.value))
                            .and(relation.MONEY_USAGE_ID.`in`(moneyUsageIdList.map { it.id })),
                    )
                    .fetch()

                result.map {
                    MoneyUsageId(
                        it.get(relation.MONEY_USAGE_ID)!!,
                    ) to
                        ImportedMailId(
                            it.get(userMails.USER_MAIL_ID)!!,
                        )
                }
                    .groupBy(
                        keySelector = { it.first },
                        valueTransform = { it.second },
                    )
            }
        }
    }

    override fun deleteMail(
        userId: UserId,
        mailId: ImportedMailId,
    ): Boolean {
        return runCatching {
            dbConnection.use { connection ->
                DSL.using(connection)
                    .deleteFrom(userMails)
                    .where(
                        DSL.value(true)
                            .and(userMails.USER_ID.eq(userId.value))
                            .and(userMails.USER_MAIL_ID.eq(mailId.id)),
                    )
                    .limit(1)
                    .execute()
            } == 1
        }.fold(
            onSuccess = { it },
            onFailure = { false },
        )
    }
}
