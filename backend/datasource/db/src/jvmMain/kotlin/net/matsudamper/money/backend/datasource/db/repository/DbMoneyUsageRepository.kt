package net.matsudamper.money.backend.datasource.db.repository

import java.lang.IllegalStateException
import java.time.LocalDateTime
import net.matsudamper.money.backend.app.interfaces.MoneyUsageRepository
import net.matsudamper.money.backend.app.interfaces.MoneyUsageRepository.OrderType
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.db.schema.tables.JMoneyUsageSubCategories
import net.matsudamper.money.db.schema.tables.JMoneyUsages
import net.matsudamper.money.db.schema.tables.JMoneyUsagesMailsRelation
import net.matsudamper.money.db.schema.tables.records.JMoneyUsagesRecord
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL
import org.jooq.kotlin.and

class DbMoneyUsageRepository : MoneyUsageRepository {
    private val jUsage = JMoneyUsages.MONEY_USAGES
    private val jSubCategory = JMoneyUsageSubCategories.MONEY_USAGE_SUB_CATEGORIES
    private val jRelation = JMoneyUsagesMailsRelation.MONEY_USAGES_MAILS_RELATION

    override fun addMailRelation(
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
                        .from(jUsage)
                        .where(
                            DSL.value(true)
                                .and(jUsage.USER_ID.eq(userId.value))
                                .and(jUsage.MONEY_USAGE_ID.eq(usageId.id)),
                        )
                        .execute()
                    if (count != 1) {
                        return@use false
                    }
                }
                run {
                    val count = DSL.using(connection)
                        .select(DSL.count())
                        .from(jRelation)
                        .where(
                            DSL.value(true)
                                .and(jRelation.USER_ID.eq(userId.value))
                                .and(jRelation.USER_MAIL_ID.eq(importedMailId.id)),
                        )
                        .execute()
                    if (count != 1) {
                        return@use false
                    }
                }

                DSL.using(connection)
                    .insertInto(jRelation)
                    .set(jRelation.USER_ID, userId.value)
                    .set(jRelation.USER_MAIL_ID, importedMailId.id)
                    .set(jRelation.MONEY_USAGE_ID, usageId.id)
                    .execute() == 1
            }
        }.onFailure {
            it.printStackTrace()
        }.fold(
            onSuccess = { it },
            onFailure = { false },
        )
    }

    override fun addUsage(
        userId: UserId,
        title: String,
        description: String,
        subCategoryId: MoneyUsageSubCategoryId?,
        date: LocalDateTime,
        amount: Int,
    ): MoneyUsageRepository.AddResult {
        return runCatching {
            DbConnectionImpl.use { connection ->
                val results = DSL.using(connection)
                    .insertInto(jUsage)
                    .set(jUsage.USER_ID, userId.value)
                    .set(jUsage.TITLE, title)
                    .set(jUsage.DESCRIPTION, description)
                    .set(jUsage.MONEY_USAGE_SUB_CATEGORY_ID, subCategoryId?.id)
                    .set(jUsage.DATETIME, date)
                    .set(jUsage.AMOUNT, amount)
                    .returningResult(jUsage)
                    .fetch()

                if (results.size != 1) {
                    throw IllegalStateException("failed to insert")
                }

                mapMoneyUsage(results.first().value1())
            }
        }
            .fold(
                onSuccess = {
                    MoneyUsageRepository.AddResult.Success(it)
                },
                onFailure = { MoneyUsageRepository.AddResult.Failed(it) },
            )
    }

    override fun getMoneyUsageByQuery(
        userId: UserId,
        size: Int,
        cursor: MoneyUsageRepository.GetMoneyUsageByQueryResult.Cursor?,
        isAsc: Boolean,
        sinceDateTime: LocalDateTime?,
        untilDateTime: LocalDateTime?,
        categoryIds: List<MoneyUsageCategoryId>,
        subCategoryIds: List<MoneyUsageSubCategoryId>,
        text: String?,
        orderType: MoneyUsageRepository.OrderType,
    ): MoneyUsageRepository.GetMoneyUsageByQueryResult {
        return runCatching {
            DbConnectionImpl.use { connection ->
                val results = DSL.using(connection)
                    .select(
                        jUsage.MONEY_USAGE_ID,
                        jUsage.DATETIME,
                        jUsage.AMOUNT,
                    )
                    .from(jUsage)
                    .leftJoin(jSubCategory).on(
                        jSubCategory.MONEY_USAGE_SUB_CATEGORY_ID
                            .eq(jUsage.MONEY_USAGE_SUB_CATEGORY_ID)
                            .and(jUsage.USER_ID.eq(userId.value)),
                    )
                    .where(
                        DSL.value(true)
                            .and(jUsage.USER_ID.eq(userId.value))
                            .and(
                                when (cursor?.lastId) {
                                    null -> DSL.value(true)
                                    else ->
                                        if (isAsc) {
                                            when (orderType) {
                                                MoneyUsageRepository.OrderType.DATE -> {
                                                    DSL.row(jUsage.DATETIME, jUsage.MONEY_USAGE_ID)
                                                        .greaterThan(cursor.date!!, cursor.lastId.id)
                                                }

                                                MoneyUsageRepository.OrderType.AMOUNT -> {
                                                    DSL.row(jUsage.AMOUNT, jUsage.MONEY_USAGE_ID)
                                                        .greaterThan(cursor.amount!!, cursor.lastId.id)
                                                }
                                            }
                                        } else {
                                            when (orderType) {
                                                MoneyUsageRepository.OrderType.DATE -> {
                                                    DSL.row(jUsage.DATETIME, jUsage.MONEY_USAGE_ID)
                                                        .lessThan(cursor.date!!, cursor.lastId.id)
                                                }

                                                MoneyUsageRepository.OrderType.AMOUNT -> {
                                                    DSL.row(jUsage.AMOUNT, jUsage.MONEY_USAGE_ID)
                                                        .lessThan(cursor.amount!!, cursor.lastId.id)
                                                }
                                            }
                                        }
                                },
                            )
                            .and(
                                when (sinceDateTime) {
                                    null -> DSL.value(true)
                                    else -> jUsage.DATETIME.greaterOrEqual(sinceDateTime)
                                },
                            )
                            .and(
                                when (untilDateTime) {
                                    null -> DSL.value(true)
                                    else -> jUsage.DATETIME.lessThan(untilDateTime)
                                },
                            )
                            .and(
                                if (categoryIds.isEmpty()) {
                                    DSL.value(true)
                                } else {
                                    jSubCategory.MONEY_USAGE_CATEGORY_ID.`in`(categoryIds.map { it.value })
                                },
                            )
                            .and(
                                if (subCategoryIds.isEmpty()) {
                                    DSL.value(true)
                                } else {
                                    jUsage.MONEY_USAGE_SUB_CATEGORY_ID.`in`(subCategoryIds.map { it.id })
                                },
                            )
                            .and(
                                when (text) {
                                    null -> DSL.value(true)
                                    else -> {
                                        jUsage.TITLE.contains(text)
                                            .or(jUsage.DESCRIPTION.contains(text))
                                    }
                                },
                            ),
                    )
                    .orderBy(
                        when (orderType) {
                            MoneyUsageRepository.OrderType.AMOUNT -> {
                                if (isAsc) {
                                    jUsage.AMOUNT.asc()
                                } else {
                                    jUsage.AMOUNT.desc()
                                }
                            }

                            MoneyUsageRepository.OrderType.DATE -> {
                                if (isAsc) {
                                    jUsage.DATETIME.asc()
                                } else {
                                    jUsage.DATETIME.desc()
                                }
                            }
                        },
                        if (isAsc) {
                            jUsage.MONEY_USAGE_ID.asc()
                        } else {
                            jUsage.MONEY_USAGE_ID.desc()
                        },
                    )
                    .limit(size)
                    .fetch()

                val resultMoneyUsageIds = results.map { result ->
                    MoneyUsageId(result.get(jUsage.MONEY_USAGE_ID)!!)
                }
                val lastDate = results.lastOrNull()?.get(jUsage.DATETIME)
                val lastAmount = results.lastOrNull()?.get(jUsage.AMOUNT)
                val cursorLastId = resultMoneyUsageIds.lastOrNull()
                MoneyUsageRepository.GetMoneyUsageByQueryResult.Success(
                    ids = resultMoneyUsageIds,
                    cursor = run cursor@{
                        when (orderType) {
                            MoneyUsageRepository.OrderType.DATE -> {
                                MoneyUsageRepository.GetMoneyUsageByQueryResult.Cursor(
                                    lastId = cursorLastId ?: return@cursor null,
                                    date = lastDate ?: return@cursor null,
                                    amount = null,
                                )
                            }

                            MoneyUsageRepository.OrderType.AMOUNT -> {
                                MoneyUsageRepository.GetMoneyUsageByQueryResult.Cursor(
                                    lastId = cursorLastId ?: return@cursor null,
                                    date = null,
                                    amount = lastAmount,
                                )
                            }
                        }
                    },
                )
            }
        }.fold(
            onSuccess = { it },
            onFailure = { e ->
                MoneyUsageRepository.GetMoneyUsageByQueryResult.Failed(e)
            },
        )
    }

    override fun getMoneyUsage(
        userId: UserId,
        ids: List<MoneyUsageId>,
    ): Result<List<MoneyUsageRepository.Usage>> {
        return runCatching {
            DbConnectionImpl.use { connection ->
                val results = DSL.using(connection)
                    .selectFrom(jUsage)
                    .where(
                        DSL.value(true)
                            .and(jUsage.USER_ID.eq(userId.value))
                            .and(jUsage.MONEY_USAGE_ID.`in`(ids.map { it.id })),
                    )
                    .fetch()

                results.map { result ->
                    mapMoneyUsage(result = result)
                }
            }
        }
    }

    private fun mapMoneyUsage(result: JMoneyUsagesRecord): MoneyUsageRepository.Usage {
        return MoneyUsageRepository.Usage(
            id = MoneyUsageId(result.get(jUsage.MONEY_USAGE_ID)!!),
            userId = UserId(result.get(jUsage.USER_ID)!!),
            title = result.get(jUsage.TITLE)!!,
            description = result.get(jUsage.DESCRIPTION)!!,
            subCategoryId = result.get(jUsage.MONEY_USAGE_SUB_CATEGORY_ID)?.let { MoneyUsageSubCategoryId(it) },
            date = result.get(jUsage.DATETIME)!!,
            amount = result.get(jUsage.AMOUNT)!!,
        )
    }

    override fun getMails(
        userId: UserId,
        importedMailId: ImportedMailId,
    ): Result<List<MoneyUsageRepository.Usage>> {
        return runCatching {
            DbConnectionImpl.use { connection ->
                DSL.using(connection)
                    .select(jUsage)
                    .from(jRelation)
                    .join(jUsage).on(
                        jRelation.MONEY_USAGE_ID.eq(jUsage.MONEY_USAGE_ID)
                            .and(jUsage.USER_ID.eq(userId.value)),
                    )
                    .where(
                        DSL.value(true)
                            .and(jRelation.USER_ID.eq(userId.value))
                            .and(jRelation.USER_MAIL_ID.eq(importedMailId.id)),
                    )
                    .fetch()
                    .map {
                        mapMoneyUsage(it.value1())
                    }
            }
        }
    }

    override fun deleteUsage(
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
                            .deleteFrom(jUsage)
                            .where(
                                DSL.value(true)
                                    .and(jUsage.USER_ID.eq(userId.value))
                                    .and(jUsage.MONEY_USAGE_ID.eq(usageId.id)),
                            )
                            .execute() != 1
                    ) {
                        context.rollback()
                        return@use false
                    }

                    context
                        .deleteFrom(jRelation)
                        .where(
                            DSL.value(true)
                                .and(jRelation.USER_ID.eq(userId.value))
                                .and(jRelation.MONEY_USAGE_ID.eq(usageId.id)),
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

    override fun updateUsage(
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
                    .update(jUsage)
                    .set(jUsage.MONEY_USAGE_ID, usageId.id)
                    .apply {
                        if (title != null) {
                            set(jUsage.TITLE, title)
                        }
                        if (description != null) {
                            set(jUsage.DESCRIPTION, description)
                        }
                        if (subCategoryId != null) {
                            set(jUsage.MONEY_USAGE_SUB_CATEGORY_ID, subCategoryId.id)
                        }
                        if (date != null) {
                            set(jUsage.DATETIME, date)
                        }
                        if (amount != null) {
                            set(jUsage.AMOUNT, amount)
                        }
                    }
                    .where(
                        DSL.value(true)
                            .and(jUsage.USER_ID.eq(userId.value))
                            .and(jUsage.MONEY_USAGE_ID.eq(usageId.id)),
                    )
                    .limit(1)
                    .execute() == 1
            }
        }.fold(
            onSuccess = { it },
            onFailure = { false },
        )
    }
}
