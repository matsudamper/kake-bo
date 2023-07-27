package net.matsudamper.money.backend.repository

import net.matsudamper.money.backend.DbConnection
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.db.schema.tables.JMoneyUsageCategories
import net.matsudamper.money.db.schema.tables.JMoneyUsageSubCategories
import net.matsudamper.money.db.schema.tables.records.JMoneyUsageCategoriesRecord
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import org.jooq.impl.DSL


class MoneyUsageSubCategoryRepository {
    private val SUB_CATEGORIES = JMoneyUsageSubCategories.MONEY_USAGE_SUB_CATEGORIES
    private val CATEGORIES = JMoneyUsageCategories.MONEY_USAGE_CATEGORIES

    fun addSubCategory(
        userId: UserId,
        name: String,
        categoryId: MoneyUsageCategoryId,
    ): AddSubCategoryResult {
        runCatching {
            DbConnection.use {
                DSL.select(CATEGORIES.MONEY_USAGE_CATEGORY_ID)
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
            DbConnection.use {
                val record = DSL.insertInto(SUB_CATEGORIES)
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

    fun getCategory(
        userId: UserId,
        moneyUsageCategoryId: MoneyUsageSubCategoryId,
    ): GetSubCategoryResult {
        return runCatching {
            DbConnection.use {
                val record = DSL.selectFrom(SUB_CATEGORIES)
                    .where(
                        SUB_CATEGORIES.USER_ID.eq(userId.id)
                            .and(SUB_CATEGORIES.MONEY_USAGE_SUB_CATEGORY_ID.eq(moneyUsageCategoryId.id)),
                    )
                    .fetchOne() ?: return@use null

                GetSubCategoryResult.Success(
                    SubCategoryResult(
                        userId = UserId(record.userId!!),
                        moneyUsageCategoryId = MoneyUsageCategoryId(record.moneyUsageCategoryId!!),
                        moneyUsageSubCategoryId = MoneyUsageSubCategoryId(record.moneyUsageSubCategoryId!!),
                        name = record.name!!,
                    ),
                )
            }
        }.fold(
            onSuccess = { it ?: GetSubCategoryResult.Failed.NotFound },
            onFailure = { GetSubCategoryResult.Failed.Error(it) },
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
        data class Success(val result: SubCategoryResult) : GetSubCategoryResult
        sealed interface Failed : GetSubCategoryResult {
            object NotFound : Failed
            data class Error(val error: Throwable) : Failed
        }
    }
}
