package net.matsudamper.money.backend.repository

import java.lang.IllegalStateException
import java.time.LocalDateTime
import net.matsudamper.money.backend.DbConnectionImpl
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.db.schema.tables.JMoneyUsages
import net.matsudamper.money.db.schema.tables.JMoneyUsagesMailsRelation
import net.matsudamper.money.db.schema.tables.records.JMoneyUsagesRecord
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import org.jooq.impl.DSL
import org.jooq.kotlin.and

class MoneyUsageRepository {
    private val usage = JMoneyUsages.MONEY_USAGES
    private val relation = JMoneyUsagesMailsRelation.MONEY_USAGES_MAILS_RELATION

    fun addMailRelation(
        userId: UserId,
        importedMailId: ImportedMailId,
        usageId: MoneyUsageId,
    ): Boolean {
        return runCatching {
            DbConnectionImpl.use { connection ->
                // 自分のものか確認する
                run {
                    val count = DSL.using(connection)
                        .select(DSL.count())
                        .from(usage)
                        .where(
                            DSL.value(true)
                                .and(usage.USER_ID.eq(userId.value))
                                .and(usage.MONEY_USAGE_ID.eq(usageId.id)),
                        )
                        .execute()
                    if (count != 1) {
                        return@use false
                    }
                }
                run {
                    val count = DSL.using(connection)
                        .select(DSL.count())
                        .from(relation)
                        .where(
                            DSL.value(true)
                                .and(relation.USER_ID.eq(userId.value))
                                .and(relation.USER_MAIL_ID.eq(importedMailId.id)),
                        )
                        .execute()
                    if (count != 1) {
                        return@use false
                    }
                }

                DSL.using(connection)
                    .insertInto(relation)
                    .set(relation.USER_ID, userId.value)
                    .set(relation.USER_MAIL_ID, importedMailId.id)
                    .set(relation.MONEY_USAGE_ID, usageId.id)
                    .execute() == 1
            }
        }.onFailure {
            it.printStackTrace()
        }.fold(
            onSuccess = { it },
            onFailure = { false },
        )
    }

    fun addUsage(
        userId: UserId,
        title: String,
        description: String,
        subCategoryId: MoneyUsageSubCategoryId?,
        date: LocalDateTime,
        amount: Int,
    ): AddResult {
        return runCatching {
            DbConnectionImpl.use { connection ->
                val results = DSL.using(connection)
                    .insertInto(usage)
                    .set(usage.USER_ID, userId.value)
                    .set(usage.TITLE, title)
                    .set(usage.DESCRIPTION, description)
                    .set(usage.MONEY_USAGE_SUB_CATEGORY_ID, subCategoryId?.id)
                    .set(usage.DATETIME, date)
                    .set(usage.AMOUNT, amount)
                    .returningResult(usage)
                    .fetch()

                if (results.size != 1) {
                    throw IllegalStateException("failed to insert")
                }

                mapMoneyUsage(results.first().value1())
            }
        }
            .fold(
                onSuccess = {
                    AddResult.Success(it)
                },
                onFailure = { AddResult.Failed(it) },
            )
    }

    fun getMoneyUsageByQuery(
        userId: UserId,
        size: Int,
        cursor: GetMoneyUsageByQueryResult.Cursor?,
        isAsc: Boolean,
        sinceDateTime: LocalDateTime?,
        untilDateTime: LocalDateTime?,
    ): GetMoneyUsageByQueryResult {
        return runCatching {
            DbConnectionImpl.use { connection ->
                val results = DSL.using(connection)
                    .select(
                        usage.MONEY_USAGE_ID,
                        usage.DATETIME,
                    )
                    .from(usage)
                    .where(
                        DSL.value(true)
                            .and(usage.USER_ID.eq(userId.value))
                            .and(
                                when (cursor?.lastId) {
                                    null -> DSL.value(true)
                                    else -> if (isAsc) {
                                        DSL.row(usage.DATETIME, usage.MONEY_USAGE_ID)
                                            .greaterThan(cursor.date, cursor.lastId.id)
                                    } else {
                                        DSL.row(usage.DATETIME, usage.MONEY_USAGE_ID)
                                            .lessThan(cursor.date, cursor.lastId.id)
                                    }
                                },
                            )
                            .and(
                                when (sinceDateTime) {
                                    null -> DSL.value(true)
                                    else -> usage.DATETIME.greaterOrEqual(sinceDateTime)
                                },
                            )
                            .and(
                                when (untilDateTime) {
                                    null -> DSL.value(true)
                                    else -> usage.DATETIME.lessThan(untilDateTime)
                                },
                            ),
                    )
                    .orderBy(
                        if (isAsc) {
                            usage.DATETIME.asc()
                        } else {
                            usage.DATETIME.desc()
                        },
                    )
                    .limit(size)
                    .fetch()

                val resultMoneyUsageIds = results.map { result ->
                    MoneyUsageId(result.get(usage.MONEY_USAGE_ID)!!)
                }
                val lastDate = results.lastOrNull()?.get(usage.DATETIME)
                val cursorLastId = resultMoneyUsageIds.lastOrNull()
                GetMoneyUsageByQueryResult.Success(
                    ids = resultMoneyUsageIds,
                    cursor = run cursor@{
                        GetMoneyUsageByQueryResult.Cursor(
                            lastId = cursorLastId ?: return@cursor null,
                            date = lastDate ?: return@cursor null,
                        )
                    },
                )
            }
        }.fold(
            onSuccess = { it },
            onFailure = { e ->
                GetMoneyUsageByQueryResult.Failed(e)
            },
        )
    }

    fun getMoneyUsage(userId: UserId, ids: List<MoneyUsageId>): Result<List<Usage>> {
        return runCatching {
            DbConnectionImpl.use { connection ->
                val results = DSL.using(connection)
                    .selectFrom(usage)
                    .where(
                        DSL.value(true)
                            .and(usage.USER_ID.eq(userId.value))
                            .and(usage.MONEY_USAGE_ID.`in`(ids.map { it.id })),
                    )
                    .fetch()

                results.map { result ->
                    mapMoneyUsage(result = result)
                }
            }
        }
    }

    private fun mapMoneyUsage(result: JMoneyUsagesRecord): Usage {
        return Usage(
            id = MoneyUsageId(result.get(usage.MONEY_USAGE_ID)!!),
            userId = UserId(result.get(usage.USER_ID)!!),
            title = result.get(usage.TITLE)!!,
            description = result.get(usage.DESCRIPTION)!!,
            subCategoryId = result.get(usage.MONEY_USAGE_SUB_CATEGORY_ID)?.let { MoneyUsageSubCategoryId(it) },
            date = result.get(usage.DATETIME)!!,
            amount = result.get(usage.AMOUNT)!!,
        )
    }

    fun getMails(
        userId: UserId,
        importedMailId: ImportedMailId,
    ): Result<List<Usage>> {
        return runCatching {
            DbConnectionImpl.use { connection ->
                DSL.using(connection)
                    .select(usage)
                    .from(relation)
                    .join(usage).on(
                        relation.MONEY_USAGE_ID.eq(usage.MONEY_USAGE_ID)
                            .and(usage.USER_ID.eq(userId.value)),
                    )
                    .where(
                        DSL.value(true)
                            .and(relation.USER_ID.eq(userId.value))
                            .and(relation.USER_MAIL_ID.eq(importedMailId.id)),
                    )
                    .fetch()
                    .map {
                        mapMoneyUsage(it.value1())
                    }
            }
        }
    }

    fun deleteUsage(
        userId: UserId,
        usageId: MoneyUsageId,
    ): Boolean {
        return runCatching {
            DbConnectionImpl.use { connection ->
                val context = DSL.using(connection)
                try {
                    context.startTransaction()

                    if (
                        context
                            .deleteFrom(usage)
                            .where(
                                DSL.value(true)
                                    .and(usage.USER_ID.eq(userId.value))
                                    .and(usage.MONEY_USAGE_ID.eq(usageId.id)),
                            )
                            .execute() != 1
                    ) {
                        context.rollback()
                        return@use false
                    }

                    context
                        .deleteFrom(relation)
                        .where(
                            DSL.value(true)
                                .and(relation.USER_ID.eq(userId.value))
                                .and(relation.MONEY_USAGE_ID.eq(usageId.id)),
                        )
                        .execute()

                    context.commit()
                    return@use true
                } catch (e: Throwable) {
                    context.rollback()
                    return@use false
                }
            }
        }.fold(
            onSuccess = { it },
            onFailure = { false },
        )
    }

    fun updateUsage(
        userId: UserId,
        usageId: MoneyUsageId,
        title: String?,
        description: String?,
        subCategoryId: MoneyUsageSubCategoryId?,
        date: LocalDateTime?,
        amount: Int?,
    ): Boolean {
        return runCatching {
            DbConnectionImpl.use { connection ->
                DSL.using(connection)
                    .update(usage)
                    .set(usage.MONEY_USAGE_ID, usageId.id)
                    .apply {
                        if (title != null) {
                            set(usage.TITLE, title)
                        }
                        if (description != null) {
                            set(usage.DESCRIPTION, description)
                        }
                        if (subCategoryId != null) {
                            set(usage.MONEY_USAGE_SUB_CATEGORY_ID, subCategoryId.id)
                        }
                        if (date != null) {
                            set(usage.DATETIME, date)
                        }
                        if (amount != null) {
                            set(usage.AMOUNT, amount)
                        }
                    }
                    .where(
                        DSL.value(true)
                            .and(usage.USER_ID.eq(userId.value))
                            .and(usage.MONEY_USAGE_ID.eq(usageId.id)),
                    )
                    .limit(1)
                    .execute() == 1
            }
        }.fold(
            onSuccess = { it },
            onFailure = { false },
        )
    }

    sealed interface AddResult {
        data class Success(val result: Usage) : AddResult
        data class Failed(val error: Throwable) : AddResult
    }

    data class Usage(
        val id: MoneyUsageId,
        val userId: UserId,
        val title: String,
        val description: String,
        val subCategoryId: MoneyUsageSubCategoryId?,
        val date: LocalDateTime,
        val amount: Int,
    )

    sealed interface GetMoneyUsageByQueryResult {
        data class Success(
            val ids: MutableList<MoneyUsageId>,
            val cursor: Cursor?,
        ) : GetMoneyUsageByQueryResult

        data class Failed(val error: Throwable) : GetMoneyUsageByQueryResult

        data class Cursor(
            val lastId: MoneyUsageId,
            val date: LocalDateTime,
        )
    }
}
