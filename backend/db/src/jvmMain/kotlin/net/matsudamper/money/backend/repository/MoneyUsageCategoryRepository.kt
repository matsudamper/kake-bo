package net.matsudamper.money.backend.repository

import net.matsudamper.money.backend.DbConnection
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.db.schema.tables.JMoneyUsageCategories
import net.matsudamper.money.db.schema.tables.records.JMoneyUsageCategoriesRecord
import net.matsudamper.money.element.MoneyUsageCategoryId
import org.jooq.impl.DSL
import org.jooq.kotlin.and

class MoneyUsageCategoryRepository {
    private val CATEGORIES = JMoneyUsageCategories.MONEY_USAGE_CATEGORIES

    fun addCategory(
        userId: UserId,
        name: String,
    ): AddCategoryResult {
        return runCatching {
            DbConnection.use { connection ->
                val result = DSL.using(connection)
                    .insertInto(CATEGORIES)
                    .set(
                        JMoneyUsageCategoriesRecord(
                            userId = userId.id,
                            name = name,
                        ),
                    )
                    .returning()
                    .fetchOne()!!

                CategoryResult(
                    userId = UserId(result.userId!!),
                    moneyUsageCategoryId = MoneyUsageCategoryId(result.moneyUsageCategoryId!!),
                    name = result.name!!,
                )
            }
        }
            .fold(
                onSuccess = { AddCategoryResult.Success(it) },
                onFailure = { AddCategoryResult.Failed(it) },
            )
    }

    fun getCategory(
        userId: UserId,
        moneyUsageCategoryIds: List<MoneyUsageCategoryId>,
    ): GetCategoryResult {
        return runCatching {
            DbConnection.use { connection ->
                val records = DSL.using(connection)
                    .selectFrom(CATEGORIES)
                    .where(
                        CATEGORIES.USER_ID.eq(userId.id)
                            .and(
                                CATEGORIES.MONEY_USAGE_CATEGORY_ID
                                    .`in`(moneyUsageCategoryIds.map { it.id }),
                            ),
                    )
                    .fetch()

                records.map { record ->
                    CategoryResult(
                        userId = UserId(record.userId!!),
                        moneyUsageCategoryId = MoneyUsageCategoryId(record.moneyUsageCategoryId!!),
                        name = record.name!!,
                    )
                }
            }
        }.fold(
            onSuccess = { GetCategoryResult.Success(it) },
            onFailure = { GetCategoryResult.Failed(it) },
        )
    }

    fun getCategory(
        userId: UserId,
    ): GetCategoryResult {
        return runCatching {
            DbConnection.use { connection ->
                val records = DSL.using(connection)
                    .selectFrom(CATEGORIES)
                    .where(
                        CATEGORIES.USER_ID.eq(userId.id),
                    )
                    .fetch()

                records.map { record ->
                    CategoryResult(
                        userId = UserId(record.userId!!),
                        moneyUsageCategoryId = MoneyUsageCategoryId(record.moneyUsageCategoryId!!),
                        name = record.name!!,
                    )
                }
            }
        }.fold(
            onSuccess = { GetCategoryResult.Success(it) },
            onFailure = { GetCategoryResult.Failed(it) },
        )
    }

    fun updateCategory(
        userId: UserId,
        categoryId: MoneyUsageCategoryId,
        name: String?,
    ): Boolean {
        return DbConnection.use { connection ->
            if (name != null) {
                DSL.using(connection)
                    .update(CATEGORIES)
                    .set(CATEGORIES.NAME, name)
                    .where(
                        DSL.value(true)
                            .and(CATEGORIES.USER_ID.eq(userId.id))
                            .and(CATEGORIES.MONEY_USAGE_CATEGORY_ID.eq(categoryId.id)),
                    )
                    .limit(1)
                    .execute()
            } else {
                0
            } >= 1
        }
    }

    data class CategoryResult(
        val userId: UserId,
        val moneyUsageCategoryId: MoneyUsageCategoryId,
        val name: String,
    )

    sealed interface AddCategoryResult {
        data class Success(val result: CategoryResult) : AddCategoryResult
        data class Failed(val error: Throwable) : AddCategoryResult
    }

    sealed interface GetCategoryResult {
        data class Success(val results: List<CategoryResult>) : GetCategoryResult
        data class Failed(val e: Throwable) : GetCategoryResult
    }
}
