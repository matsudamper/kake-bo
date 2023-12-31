package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.datasource.db.DbConnection
import net.matsudamper.money.backend.datasource.db.element.ImportedMailCategoryFilterConditionType
import net.matsudamper.money.backend.datasource.db.element.ImportedMailCategoryFilterDatasourceType
import net.matsudamper.money.backend.datasource.db.element.ImportedMailFilterCategoryConditionOperator
import net.matsudamper.money.db.schema.tables.JCategoryMailFilterConditions
import net.matsudamper.money.db.schema.tables.JCategoryMailFilters
import net.matsudamper.money.db.schema.tables.records.JCategoryMailFilterConditionsRecord
import net.matsudamper.money.db.schema.tables.records.JCategoryMailFiltersRecord
import net.matsudamper.money.element.ImportedMailCategoryFilterConditionId
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL
import org.jooq.kotlin.and

class MailFilterRepository(
    private val dbConnection: DbConnection,
) {
    private val filters = JCategoryMailFilters.CATEGORY_MAIL_FILTERS
    private val conditions = JCategoryMailFilterConditions.CATEGORY_MAIL_FILTER_CONDITIONS

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
                            userId = userId.value,
                            orderNumber = orderNum,
                            categoryMailFilterConditionOperatorTypeId = ImportedMailFilterCategoryConditionOperator.AND.dbValue,
                        ),
                    )
                    .returning(filters)
                    .fetchOne()
                result ?: throw IllegalStateException("insert failed")
                mapResult(result)
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
                            .and(filters.USER_ID.eq(userId.value))
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
                            .and(filters.USER_ID.eq(userId.value))
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

    fun getConditions(userId: UserId, filterId: ImportedMailCategoryFilterId): Result<MailFilterConditionResult> {
        return dbConnection.use {
            runCatching {
                val result = DSL.using(it)
                    .selectFrom(conditions)
                    .where(
                        DSL.value(true)
                            .and(conditions.USER_ID.eq(userId.value))
                            .and(conditions.CATEGORY_MAIL_FILTER_ID.eq(filterId.id)),
                    )
                    .fetch()

                MailFilterConditionResult(
                    filterId = filterId,
                    conditions = result.map { mapResult(it) },
                )
            }
        }
    }

    fun getConditions(userId: UserId, filterIds: List<ImportedMailCategoryFilterConditionId>): Result<List<Condition>> {
        return dbConnection.use {
            runCatching {
                val result = DSL.using(it)
                    .selectFrom(conditions)
                    .where(
                        DSL.value(true)
                            .and(conditions.USER_ID.eq(userId.value))
                            .and(conditions.CATEGORY_MAIL_FILTER_CONDITION_ID.`in`(filterIds.map { it.id })),
                    )
                    .fetch()

                result.map { mapResult(it) }
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
            operator = ImportedMailFilterCategoryConditionOperator.fromDbValue(
                record.get(filters.CATEGORY_MAIL_FILTER_CONDITION_OPERATOR_TYPE_ID)!!,
            ),
            orderNumber = record.get(filters.ORDER_NUMBER)!!,
        )
    }

    private fun mapResult(record: JCategoryMailFilterConditionsRecord): Condition {
        return Condition(
            filterId = ImportedMailCategoryFilterId(record.get(conditions.CATEGORY_MAIL_FILTER_ID)!!),
            conditionId = ImportedMailCategoryFilterConditionId(record.get(conditions.CATEGORY_MAIL_FILTER_CONDITION_ID)!!),
            text = record.get(conditions.TEXT)!!,
            conditionType = ImportedMailCategoryFilterConditionType.fromDbValue(
                record.get(conditions.CATEGORY_MAIL_FILTER_CONDITION_TYPE_ID)!!,
            ),
            dataSourceType = ImportedMailCategoryFilterDatasourceType.fromDbValue(
                record.get(conditions.CATEGORY_MAIL_FILTER_DATASOURCE_TYPE_ID)!!,
            ),
        )
    }

    /**
     * @return update success or not
     */
    fun updateFilter(
        filterId: ImportedMailCategoryFilterId,
        userId: UserId,
        title: String? = null,
        orderNum: Int? = null,
        subCategory: MoneyUsageSubCategoryId? = null,
        operator: ImportedMailFilterCategoryConditionOperator? = null,
    ): Boolean {
        return runCatching {
            dbConnection.use {
                val resultCount = DSL.using(it)
                    .update(filters)
                    .set(
                        buildMap {
                            if (title != null) {
                                put(filters.TITLE, title)
                            }
                            if (orderNum != null) {
                                put(filters.ORDER_NUMBER, orderNum)
                            }
                            if (subCategory != null) {
                                put(filters.MONEY_USAGE_SUB_CATEGORY_ID, subCategory.id)
                            }
                            if (operator != null) {
                                put(filters.CATEGORY_MAIL_FILTER_CONDITION_OPERATOR_TYPE_ID, operator.dbValue)
                            }
                        },
                    )
                    .where(
                        DSL.value(true)
                            .and(filters.USER_ID.eq(userId.value))
                            .and(filters.CATEGORY_MAIL_FILTER_ID.eq(filterId.id)),
                    )
                    .limit(1)
                    .execute()

                resultCount == 1
            }
        }.fold(
            onSuccess = { it },
            onFailure = { false },
        )
    }

    fun deleteFilter(filterId: ImportedMailCategoryFilterId, userId: UserId): Boolean {
        return runCatching {
            dbConnection.use {
                DSL.using(it)
                    .deleteFrom(conditions)
                    .where(
                        DSL.value(true)
                            .and(conditions.USER_ID.eq(userId.value))
                            .and(conditions.CATEGORY_MAIL_FILTER_ID.eq(filterId.id)),
                    )
                    .execute()

                val resultCount = DSL.using(it)
                    .deleteFrom(filters)
                    .where(
                        DSL.value(true)
                            .and(filters.USER_ID.eq(userId.value))
                            .and(filters.CATEGORY_MAIL_FILTER_ID.eq(filterId.id)),
                    )
                    .limit(1)
                    .execute()

                resultCount == 1
            }
        }.fold(
            onSuccess = { it },
            onFailure = { false },
        )
    }

    /**
     * @return insert success or not
     */
    fun addCondition(
        userId: UserId,
        filterId: ImportedMailCategoryFilterId,
        condition: ImportedMailCategoryFilterConditionType?,
        text: String?,
        dataSource: ImportedMailCategoryFilterDatasourceType?,
    ): Boolean {
        return runCatching {
            // TODO filterIdが存在するかチェックする
            dbConnection.use {
                val resultRowCount = DSL.using(it)
                    .insertInto(conditions)
                    .set(
                        buildMap {
                            put(conditions.USER_ID, userId.value)
                            put(conditions.CATEGORY_MAIL_FILTER_ID, filterId.id)
                            put(
                                conditions.CATEGORY_MAIL_FILTER_CONDITION_TYPE_ID,
                                (condition ?: ImportedMailCategoryFilterConditionType.Include).dbValue,
                            )
                            put(conditions.TEXT, text.orEmpty())
                            put(
                                conditions.CATEGORY_MAIL_FILTER_DATASOURCE_TYPE_ID,
                                (dataSource ?: ImportedMailCategoryFilterDatasourceType.Title).dbValue,
                            )
                        },
                    )
                    .execute()
                resultRowCount == 1
            }
        }.onFailure {
            it.printStackTrace()
        }.fold(
            onSuccess = { it },
            onFailure = { false },
        )
    }

    fun updateCondition(
        userId: UserId,
        conditionId: ImportedMailCategoryFilterConditionId,
        text: String?,
        conditionType: ImportedMailCategoryFilterConditionType?,
        dataSource: ImportedMailCategoryFilterDatasourceType?,
    ): Boolean {
        return runCatching {
            dbConnection.use {
                val resultRowCount = DSL.using(it)
                    .update(conditions)
                    .set(
                        buildMap {
                            if (text != null) {
                                put(conditions.TEXT, text)
                            }
                            if (conditionType != null) {
                                put(conditions.CATEGORY_MAIL_FILTER_CONDITION_TYPE_ID, conditionType.dbValue)
                            }
                            if (dataSource != null) {
                                put(conditions.CATEGORY_MAIL_FILTER_DATASOURCE_TYPE_ID, dataSource.dbValue)
                            }
                        },
                    )
                    .where(
                        DSL.value(true)
                            .and(conditions.USER_ID.eq(userId.value))
                            .and(conditions.CATEGORY_MAIL_FILTER_CONDITION_ID.eq(conditionId.id)),
                    )
                    .limit(1)
                    .execute()
                resultRowCount == 1
            }
        }.onFailure {
            it.printStackTrace()
        }.fold(
            onSuccess = { it },
            onFailure = { false },
        )
    }

    fun deleteCondition(
        userId: UserId,
        conditionId: ImportedMailCategoryFilterConditionId,
    ): Boolean {
        return runCatching {
            dbConnection.use {
                val resultRowCount = DSL.using(it)
                    .deleteFrom(conditions)
                    .where(
                        DSL.value(true)
                            .and(conditions.USER_ID.eq(userId.value))
                            .and(conditions.CATEGORY_MAIL_FILTER_CONDITION_ID.eq(conditionId.id)),
                    )
                    .limit(1)
                    .execute()
                resultRowCount == 1
            }
        }.onFailure {
            it.printStackTrace()
        }.fold(
            onSuccess = { it },
            onFailure = { false },
        )
    }

    fun getFilters(userId: UserId): List<MailFilter> {
        return runCatching {
            dbConnection.use {
                DSL.using(it)
                    .selectFrom(filters)
                    .where(
                        DSL.value(true)
                            .and(filters.USER_ID.eq(userId.value)),
                    )
                    .orderBy(filters.ORDER_NUMBER.asc())
                    .fetch()
                    .map { record ->
                        mapResult(record)
                    }
            }
        }.onFailure {
            it.printStackTrace()
        }.fold(
            onSuccess = { it },
            onFailure = { listOf() },
        )
    }

    fun getConditions(userId: UserId): List<Condition> {
        return runCatching {
            dbConnection.use {
                DSL.using(it)
                    .selectFrom(conditions)
                    .where(
                        DSL.value(true)
                            .and(conditions.USER_ID.eq(userId.value)),
                    )
                    .fetch()
                    .map { record ->
                        mapResult(record)
                    }
            }
        }.onFailure {
            it.printStackTrace()
        }.fold(
            onSuccess = { it },
            onFailure = { listOf() },
        )
    }

    data class MailFiltersResult(
        val items: List<MailFilter>,
        val cursor: MailFilterCursor?,
    )

    data class MailFilterConditionResult(
        val filterId: ImportedMailCategoryFilterId,
        val conditions: List<Condition>,
    )

    data class MailFilter(
        val importedMailCategoryFilterId: ImportedMailCategoryFilterId,
        val userId: UserId,
        val title: String,
        val moneyUsageSubCategoryId: MoneyUsageSubCategoryId?,
        val operator: ImportedMailFilterCategoryConditionOperator,
        val orderNumber: Int,
    )

    data class Condition(
        val filterId: ImportedMailCategoryFilterId,
        val conditionId: ImportedMailCategoryFilterConditionId,
        val text: String,
        val conditionType: ImportedMailCategoryFilterConditionType,
        val dataSourceType: ImportedMailCategoryFilterDatasourceType,
    )

    data class MailFilterCursor(
        val id: ImportedMailCategoryFilterId,
        val orderNumber: Int,
    )
}
