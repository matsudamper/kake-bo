package net.matsudamper.money.backend.repository

import net.matsudamper.money.backend.DbConnectionImpl
import net.matsudamper.money.element.UserId
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
            DbConnectionImpl.use { connection ->
                DSL.using(connection)
                    .select(CATEGORIES.MONEY_USAGE_CATEGORY_ID)
                    .from(CATEGORIES)
                    .where(
                        CATEGORIES.USER_ID.eq(userId.value)
                            .and(CATEGORIES.MONEY_USAGE_CATEGORY_ID.eq(categoryId.value)),
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
            DbConnectionImpl.use { connection ->
                val record = DSL.using(connection)
                    .insertInto(SUB_CATEGORIES)
                    .set(
                        JMoneyUsageCategoriesRecord(
                            userId = userId.value,
                            name = name,
                            moneyUsageCategoryId = categoryId.value,
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
            val isOwner = DbConnectionImpl.use { connection ->
                DSL.using(connection)
                    .select()
                    .from(CATEGORIES)
                    .where(
                        DSL.value(true)
                            .and(CATEGORIES.USER_ID.eq(userId.value))
                            .and(CATEGORIES.MONEY_USAGE_CATEGORY_ID.eq(categoryId.value)),
                    )
                    .fetchOne() != null
            }

            if (isOwner.not()) {
                return GetSubCategoryResult.Failed(IllegalArgumentException("categoryId=$categoryId is Not owner."))
            }

            DbConnectionImpl.use { connection ->
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
                            .and(CATEGORIES.USER_ID.eq(userId.value))
                            .and(SUB_CATEGORIES.USER_ID.eq(userId.value))
                            .and(CATEGORIES.MONEY_USAGE_CATEGORY_ID.eq(categoryId.value)),
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
            DbConnectionImpl.use { connection ->
                val records = DSL.using(connection)
                    .selectFrom(SUB_CATEGORIES)
                    .where(
                        SUB_CATEGORIES.USER_ID.eq(userId.value)
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

    fun updateSubCategory(userId: UserId, subCategoryId: MoneyUsageSubCategoryId, name: String?): Boolean {
        return DbConnectionImpl.use { connection ->
            if (name == null) {
                0
            } else {
                DSL.using(connection)
                    .update(SUB_CATEGORIES)
                    .set(SUB_CATEGORIES.NAME, name)
                    .where(
                        DSL.value(true)
                            .and(SUB_CATEGORIES.USER_ID.eq(userId.value))
                            .and(SUB_CATEGORIES.MONEY_USAGE_SUB_CATEGORY_ID.eq(subCategoryId.id)),
                    )
                    .limit(1)
                    .execute()
            } >= 1
        }
    }

    fun deleteSubCategory(
        userId: UserId,
        subCategoryId: MoneyUsageSubCategoryId,
    ): Boolean {
        return DbConnectionImpl.use { connection ->
            DSL.using(connection)
                .deleteFrom(SUB_CATEGORIES)
                .where(
                    DSL.value(true)
                        .and(SUB_CATEGORIES.USER_ID.eq(userId.value))
                        .and(SUB_CATEGORIES.MONEY_USAGE_SUB_CATEGORY_ID.eq(subCategoryId.id)),
                )
                .limit(1)
                .execute()
        } >= 1
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
