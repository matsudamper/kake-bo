package net.matsudamper.money.backend.repository

import net.matsudamper.money.backend.DbConnection
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.db.schema.tables.JMoneyUsageCategories
import net.matsudamper.money.db.schema.tables.JMoneyUsageSubCategories
import net.matsudamper.money.db.schema.tables.records.JMoneyUsageCategoriesRecord
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import org.jooq.impl.DSL
import org.jooq.kotlin.and

class MoneyUsageSubCategoryRepository {
    private val SUB_CATEGORIES = JMoneyUsageSubCategories.MONEY_USAGE_SUB_CATEGORIES
    private val CATEGORIES = JMoneyUsageCategories.MONEY_USAGE_CATEGORIES

    fun addSubCategory(
        userId: UserId,
        name: String,
        categoryId: MoneyUsageCategoryId,
    ): AddSubCategoryResult {
        runCatching {
            DbConnection.use { connection ->
                DSL.using(connection)
                    .select(CATEGORIES.MONEY_USAGE_CATEGORY_ID)
                    .from(CATEGORIES)
                    .where(
                        CATEGORIES.USER_ID.eq(userId.id)
                            .and(CATEGORIES.MONEY_USAGE_CATEGORY_ID.eq(categoryId.id)),
                    )
                    .count()
            }
        }.onFailure {
            return AddSubCategoryResult.Failed.Error(it)
        }.onSuccess {
            if (it == 0) {
                return AddSubCategoryResult.Failed.CategoryNotFound
            }
        }

        return runCatching {
            DbConnection.use { connection ->
                val record = DSL.using(connection)
                    .insertInto(SUB_CATEGORIES)
                    .set(
                        JMoneyUsageCategoriesRecord(
                            userId = userId.id,
                            name = name,
                            moneyUsageCategoryId = categoryId.id,
                        ),
                    )
                    .returning()
                    .fetchOne()!!

                SubCategoryResult(
                    userId = UserId(record.userId!!),
                    moneyUsageCategoryId = MoneyUsageCategoryId(record.moneyUsageCategoryId!!),
                    moneyUsageSubCategoryId = MoneyUsageSubCategoryId(record.moneyUsageSubCategoryId!!),
                    name = record.name!!,
                )
            }
        }
            .fold(
                onSuccess = { AddSubCategoryResult.Success(it) },
                onFailure = { AddSubCategoryResult.Failed.Error(it) },
            )
    }

    fun getSubCategory(
        userId: UserId,
        categoryId: MoneyUsageCategoryId,
    ): GetSubCategoryResult {
        return runCatching {
            val isOwner = DbConnection.use { connection ->
                DSL.using(connection)
                    .select()
                    .from(CATEGORIES)
                    .where(
                        DSL.value(true)
                            .and(CATEGORIES.USER_ID.eq(userId.id))
                            .and(CATEGORIES.MONEY_USAGE_CATEGORY_ID.eq(categoryId.id))
                    )
                    .fetchOne() != null
            }

            if (isOwner.not()) {
                return GetSubCategoryResult.Failed(IllegalArgumentException("categoryId=$categoryId is Not owner."))
            }

            DbConnection.use { connection ->
                val records = DSL.using(connection)
                    .select(
                        SUB_CATEGORIES.USER_ID,
                        SUB_CATEGORIES.MONEY_USAGE_SUB_CATEGORY_ID,
                        SUB_CATEGORIES.MONEY_USAGE_CATEGORY_ID,
                        SUB_CATEGORIES.NAME,
                    )
                    .from(SUB_CATEGORIES)
                    .join(CATEGORIES).using(CATEGORIES.MONEY_USAGE_CATEGORY_ID)
                    .where(
                        DSL.value(true)
                            .and(CATEGORIES.USER_ID.eq(userId.id))
                            .and(SUB_CATEGORIES.USER_ID.eq(userId.id))
                            .and(CATEGORIES.MONEY_USAGE_CATEGORY_ID.eq(categoryId.id)),
                    )
                    .fetch()

                records.map { record ->
                    SubCategoryResult(
                        userId = UserId(record.get(SUB_CATEGORIES.USER_ID)!!),
                        moneyUsageCategoryId = MoneyUsageCategoryId(
                            record.get(SUB_CATEGORIES.MONEY_USAGE_CATEGORY_ID)!!,
                        ),
                        moneyUsageSubCategoryId = MoneyUsageSubCategoryId(
                            record.get(SUB_CATEGORIES.MONEY_USAGE_SUB_CATEGORY_ID)!!,
                        ),
                        name = record.get(SUB_CATEGORIES.NAME)!!,
                    )
                }
            }
        }.fold(
            onSuccess = { GetSubCategoryResult.Success(it) },
            onFailure = { GetSubCategoryResult.Failed(it) },
        )
    }

    fun getSubCategory(
        userId: UserId,
        moneyUsageSubCategoryIds: List<MoneyUsageSubCategoryId>,
    ): GetSubCategoryResult {
        return runCatching {
            DbConnection.use { connection ->
                val records = DSL.using(connection)
                    .selectFrom(SUB_CATEGORIES)
                    .where(
                        SUB_CATEGORIES.USER_ID.eq(userId.id)
                            .and(
                                SUB_CATEGORIES.MONEY_USAGE_SUB_CATEGORY_ID
                                    .`in`(moneyUsageSubCategoryIds.map { it.id }),
                            ),
                    )
                    .fetch()

                records.map { record ->
                    SubCategoryResult(
                        userId = UserId(record.userId!!),
                        moneyUsageCategoryId = MoneyUsageCategoryId(record.moneyUsageCategoryId!!),
                        moneyUsageSubCategoryId = MoneyUsageSubCategoryId(record.moneyUsageSubCategoryId!!),
                        name = record.name!!,
                    )
                }
            }
        }.fold(
            onSuccess = { GetSubCategoryResult.Success(it) },
            onFailure = { GetSubCategoryResult.Failed(it) },
        )
    }

    data class SubCategoryResult(
        val userId: UserId,
        val moneyUsageCategoryId: MoneyUsageCategoryId,
        val moneyUsageSubCategoryId: MoneyUsageSubCategoryId,
        val name: String,
    )

    sealed interface AddSubCategoryResult {
        data class Success(val result: SubCategoryResult) : AddSubCategoryResult
        sealed interface Failed : AddSubCategoryResult {
            object CategoryNotFound : AddSubCategoryResult
            data class Error(val error: Throwable) : AddSubCategoryResult
        }
    }

    sealed interface GetSubCategoryResult {
        data class Success(val results: List<SubCategoryResult>) : GetSubCategoryResult
        data class Failed(val e: Throwable) : GetSubCategoryResult
    }
}
