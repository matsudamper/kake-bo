package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.app.interfaces.MoneyUsageSubCategoryRepository
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.db.schema.tables.JMoneyUsageCategories
import net.matsudamper.money.db.schema.tables.JMoneyUsageSubCategories
import net.matsudamper.money.db.schema.tables.records.JMoneyUsageCategoriesRecord
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL
import org.jooq.kotlin.and

class DbMoneyUsageSubCategoryRepository : MoneyUsageSubCategoryRepository {
    private val subCategories = JMoneyUsageSubCategories.MONEY_USAGE_SUB_CATEGORIES
    private val categories = JMoneyUsageCategories.MONEY_USAGE_CATEGORIES

    override fun addSubCategory(
        userId: UserId,
        name: String,
        categoryId: MoneyUsageCategoryId,
    ): MoneyUsageSubCategoryRepository.AddSubCategoryResult {
        runCatching {
            DbConnectionImpl.use { connection ->
                DSL.using(connection)
                    .select(categories.MONEY_USAGE_CATEGORY_ID)
                    .from(categories)
                    .where(
                        categories.USER_ID.eq(userId.value)
                            .and(categories.MONEY_USAGE_CATEGORY_ID.eq(categoryId.value)),
                    )
                    .count()
            }
        }.onFailure {
            return MoneyUsageSubCategoryRepository.AddSubCategoryResult.Failed.Error(it)
        }.onSuccess {
            if (it == 0) {
                return MoneyUsageSubCategoryRepository.AddSubCategoryResult.Failed.CategoryNotFound
            }
        }

        return runCatching {
            DbConnectionImpl.use { connection ->
                val record =
                    DSL.using(connection)
                        .insertInto(subCategories)
                        .set(
                            JMoneyUsageCategoriesRecord(
                                userId = userId.value,
                                name = name,
                                moneyUsageCategoryId = categoryId.value,
                            ),
                        )
                        .returning()
                        .fetchOne()!!

                MoneyUsageSubCategoryRepository.SubCategoryResult(
                    userId = UserId(record.userId!!),
                    moneyUsageCategoryId = MoneyUsageCategoryId(record.moneyUsageCategoryId!!),
                    moneyUsageSubCategoryId = MoneyUsageSubCategoryId(record.moneyUsageSubCategoryId!!),
                    name = record.name!!,
                )
            }
        }
            .fold(
                onSuccess = { MoneyUsageSubCategoryRepository.AddSubCategoryResult.Success(it) },
                onFailure = { MoneyUsageSubCategoryRepository.AddSubCategoryResult.Failed.Error(it) },
            )
    }

    override fun getSubCategory(
        userId: UserId,
        categoryId: MoneyUsageCategoryId,
    ): MoneyUsageSubCategoryRepository.GetSubCategoryResult {
        return runCatching {
            val isOwner =
                DbConnectionImpl.use { connection ->
                    DSL.using(connection)
                        .select()
                        .from(categories)
                        .where(
                            DSL.value(true)
                                .and(categories.USER_ID.eq(userId.value))
                                .and(categories.MONEY_USAGE_CATEGORY_ID.eq(categoryId.value)),
                        )
                        .fetchOne() != null
                }

            if (isOwner.not()) {
                return MoneyUsageSubCategoryRepository.GetSubCategoryResult.Failed(IllegalArgumentException("categoryId=$categoryId is Not owner."))
            }

            DbConnectionImpl.use { connection ->
                val records =
                    DSL.using(connection)
                        .select(
                            subCategories.USER_ID,
                            subCategories.MONEY_USAGE_SUB_CATEGORY_ID,
                            subCategories.MONEY_USAGE_CATEGORY_ID,
                            subCategories.NAME,
                        )
                        .from(subCategories)
                        .join(categories).using(categories.MONEY_USAGE_CATEGORY_ID)
                        .where(
                            DSL.value(true)
                                .and(categories.USER_ID.eq(userId.value))
                                .and(subCategories.USER_ID.eq(userId.value))
                                .and(categories.MONEY_USAGE_CATEGORY_ID.eq(categoryId.value)),
                        )
                        .fetch()

                records.map { record ->
                    MoneyUsageSubCategoryRepository.SubCategoryResult(
                        userId = UserId(record.get(subCategories.USER_ID)!!),
                        moneyUsageCategoryId =
                        MoneyUsageCategoryId(
                            record.get(subCategories.MONEY_USAGE_CATEGORY_ID)!!,
                        ),
                        moneyUsageSubCategoryId =
                        MoneyUsageSubCategoryId(
                            record.get(subCategories.MONEY_USAGE_SUB_CATEGORY_ID)!!,
                        ),
                        name = record.get(subCategories.NAME)!!,
                    )
                }
            }
        }.fold(
            onSuccess = { MoneyUsageSubCategoryRepository.GetSubCategoryResult.Success(it) },
            onFailure = { MoneyUsageSubCategoryRepository.GetSubCategoryResult.Failed(it) },
        )
    }

    override fun getSubCategory(
        userId: UserId,
        moneyUsageSubCategoryIds: List<MoneyUsageSubCategoryId>,
    ): MoneyUsageSubCategoryRepository.GetSubCategoryResult {
        return runCatching {
            DbConnectionImpl.use { connection ->
                val records =
                    DSL.using(connection)
                        .selectFrom(subCategories)
                        .where(
                            subCategories.USER_ID.eq(userId.value)
                                .and(
                                    subCategories.MONEY_USAGE_SUB_CATEGORY_ID
                                        .`in`(moneyUsageSubCategoryIds.map { it.id }),
                                ),
                        )
                        .fetch()

                records.map { record ->
                    MoneyUsageSubCategoryRepository.SubCategoryResult(
                        userId = UserId(record.userId!!),
                        moneyUsageCategoryId = MoneyUsageCategoryId(record.moneyUsageCategoryId!!),
                        moneyUsageSubCategoryId = MoneyUsageSubCategoryId(record.moneyUsageSubCategoryId!!),
                        name = record.name!!,
                    )
                }
            }
        }.fold(
            onSuccess = { MoneyUsageSubCategoryRepository.GetSubCategoryResult.Success(it) },
            onFailure = { MoneyUsageSubCategoryRepository.GetSubCategoryResult.Failed(it) },
        )
    }

    override fun updateSubCategory(
        userId: UserId,
        subCategoryId: MoneyUsageSubCategoryId,
        name: String?,
    ): Boolean {
        return DbConnectionImpl.use { connection ->
            if (name == null) {
                0
            } else {
                DSL.using(connection)
                    .update(subCategories)
                    .set(subCategories.NAME, name)
                    .where(
                        DSL.value(true)
                            .and(subCategories.USER_ID.eq(userId.value))
                            .and(subCategories.MONEY_USAGE_SUB_CATEGORY_ID.eq(subCategoryId.id)),
                    )
                    .limit(1)
                    .execute()
            } >= 1
        }
    }

    override fun deleteSubCategory(
        userId: UserId,
        subCategoryId: MoneyUsageSubCategoryId,
    ): Boolean {
        return DbConnectionImpl.use { connection ->
            DSL.using(connection)
                .deleteFrom(subCategories)
                .where(
                    DSL.value(true)
                        .and(subCategories.USER_ID.eq(userId.value))
                        .and(subCategories.MONEY_USAGE_SUB_CATEGORY_ID.eq(subCategoryId.id)),
                )
                .limit(1)
                .execute()
        } >= 1
    }
}
