package net.matsudamper.money.backend.repository

import net.matsudamper.money.backend.DbConnection
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.db.schema.tables.JCategoryMailFilterConditionGroups
import net.matsudamper.money.db.schema.tables.JCategoryMailFilters
import net.matsudamper.money.db.schema.tables.records.JCategoryMailFiltersRecord
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import org.jooq.impl.DSL
import org.jooq.kotlin.and

class MailFilterRepository(
    private val dbConnection: DbConnection,
) {
    private val filters = JCategoryMailFilters.CATEGORY_MAIL_FILTERS
    private val conditionGroups = JCategoryMailFilterConditionGroups.CATEGORY_MAIL_FILTER_CONDITION_GROUPS

    fun addFilter(
        title: String,
        userId: UserId,
        orderNum: Int,
    ): Result<MailFilter> {
        return dbConnection.use {
            runCatching {
                val result = DSL.using(it)
                    .insertInto(filters)
                    .set(
                        JCategoryMailFiltersRecord(
                            title = title,
                            userId = userId.id,
                            orderNumber = orderNum,
                        ),
                    )
                    .returning(filters)
                    .fetchOne()
                result ?: throw IllegalStateException("insert failed")
                MailFilter(
                    importedMailCategoryFilterId = ImportedMailCategoryFilterId(result.categoryMailFilterId!!),
                    userId = UserId(result.userId!!),
                    title = result.title.orEmpty(),
                    moneyUsageSubCategoryId = MoneyUsageSubCategoryId(result.moneyUsageSubCategoryId!!),
                    orderNumber = result.orderNumber!!,
                )
            }
        }
    }

    fun getFilters(
        userId: UserId,
        categoryFilterIds: List<ImportedMailCategoryFilterId>,
    ): Result<List<MailFilter>> {
        return dbConnection.use {
            runCatching {
                DSL.using(it)
                    .selectFrom(filters)
                    .where(
                        DSL.value(true)
                            .and(filters.USER_ID.eq(userId.id))
                            .and(filters.CATEGORY_MAIL_FILTER_ID.`in`(categoryFilterIds.map { it.id })),
                    )
                    .fetch()
                    .map {
                        mapResult(it)
                    }
            }
        }
    }

    fun getFilters(
        isAsc: Boolean,
        userId: UserId,
        cursor: MailFilterCursor?,
    ): Result<MailFiltersResult> {
        return dbConnection.use {
            runCatching {
                val result = DSL.using(it)
                    .selectFrom(filters)
                    .where(
                        DSL.value(true)
                            .and(filters.USER_ID.eq(userId.id))
                            .and(
                                if (cursor == null) {
                                    DSL.value(true)
                                } else {
                                    if (isAsc) {
                                        DSL.row(filters.ORDER_NUMBER, filters.CATEGORY_MAIL_FILTER_ID)
                                            .gt(cursor.orderNumber, cursor.id.id)
                                    } else {
                                        DSL.row(filters.ORDER_NUMBER, filters.CATEGORY_MAIL_FILTER_ID)
                                            .lt(cursor.orderNumber, cursor.id.id)
                                    }
                                },
                            ),
                    )
                    .orderBy(
                        // 基本はASCであり、追加直後の要素を先頭にするためにDESCにする
                        if (isAsc) {
                            filters.CREATED_DATETIME.desc()
                        } else {
                            filters.CREATED_DATETIME.asc()
                        },
                        filters.ORDER_NUMBER.asc(),
                    )
                    .fetch()
                    .map { mapResult(it) }

                val lastResult = result.lastOrNull()
                MailFiltersResult(
                    items = result,
                    cursor = if (lastResult == null) {
                        cursor
                    } else {
                        MailFilterCursor(
                            id = lastResult.importedMailCategoryFilterId,
                            orderNumber = lastResult.orderNumber,
                        )
                    },
                )
            }
        }
    }

    private fun mapResult(record: JCategoryMailFiltersRecord): MailFilter {
        return MailFilter(
            importedMailCategoryFilterId = ImportedMailCategoryFilterId(record.get(filters.CATEGORY_MAIL_FILTER_ID)!!),
            userId = UserId(record.get(filters.USER_ID)!!),
            title = record.get(filters.TITLE)!!,
            moneyUsageSubCategoryId = record.get(filters.MONEY_USAGE_SUB_CATEGORY_ID)?.let {
                MoneyUsageSubCategoryId(it)
            },
            orderNumber = record.get(filters.ORDER_NUMBER)!!,
        )
    }

    data class MailFiltersResult(
        val items: List<MailFilter>,
        val cursor: MailFilterCursor?,
    )

    data class MailFilter(
        val importedMailCategoryFilterId: ImportedMailCategoryFilterId,
        val userId: UserId,
        val title: String,
        val moneyUsageSubCategoryId: MoneyUsageSubCategoryId?,
        val orderNumber: Int,
    )

    data class MailFilterCursor(
        val id: ImportedMailCategoryFilterId,
        val orderNumber: Int,
    )
}
