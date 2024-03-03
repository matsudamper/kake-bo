package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.app.interfaces.MoneyUsageCategoryRepository
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.db.schema.tables.JMoneyUsageCategories
import net.matsudamper.money.db.schema.tables.records.JMoneyUsageCategoriesRecord
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL
import org.jooq.kotlin.and

class DbMoneyUsageCategoryRepository : MoneyUsageCategoryRepository {
    private val CATEGORIES = JMoneyUsageCategories.MONEY_USAGE_CATEGORIES

    override fun addCategory(
        userId: UserId,
        name: String,
    ): MoneyUsageCategoryRepository.AddCategoryResult {
        return runCatching {
            DbConnectionImpl.use { connection ->
                val result =
                    DSL.using(connection)
                        .insertInto(CATEGORIES)
                        .set(
                            JMoneyUsageCategoriesRecord(
                                userId = userId.value,
                                name = name,
                            ),
                        )
                        .returning()
                        .fetchOne()!!

                MoneyUsageCategoryRepository.CategoryResult(
                    userId = UserId(result.userId!!),
                    moneyUsageCategoryId = MoneyUsageCategoryId(result.moneyUsageCategoryId!!),
                    name = result.name!!,
                )
            }
        }
            .fold(
                onSuccess = { MoneyUsageCategoryRepository.AddCategoryResult.Success(it) },
                onFailure = { MoneyUsageCategoryRepository.AddCategoryResult.Failed(it) },
            )
    }

    override fun getCategory(
        userId: UserId,
        moneyUsageCategoryIds: List<MoneyUsageCategoryId>,
    ): MoneyUsageCategoryRepository.GetCategoryResult {
        return runCatching {
            DbConnectionImpl.use { connection ->
                val records =
                    DSL.using(connection)
                        .selectFrom(CATEGORIES)
                        .where(
                            CATEGORIES.USER_ID.eq(userId.value)
                                .and(
                                    CATEGORIES.MONEY_USAGE_CATEGORY_ID
                                        .`in`(moneyUsageCategoryIds.map { it.value }),
                                ),
                        )
                        .fetch()

                records.map { record ->
                    MoneyUsageCategoryRepository.CategoryResult(
                        userId = UserId(record.userId!!),
                        moneyUsageCategoryId = MoneyUsageCategoryId(record.moneyUsageCategoryId!!),
                        name = record.name!!,
                    )
                }
            }
        }.fold(
            onSuccess = { MoneyUsageCategoryRepository.GetCategoryResult.Success(it) },
            onFailure = { MoneyUsageCategoryRepository.GetCategoryResult.Failed(it) },
        )
    }

    override fun getCategory(userId: UserId): MoneyUsageCategoryRepository.GetCategoryResult {
        return runCatching {
            DbConnectionImpl.use { connection ->
                val records =
                    DSL.using(connection)
                        .selectFrom(CATEGORIES)
                        .where(
                            CATEGORIES.USER_ID.eq(userId.value),
                        )
                        .fetch()

                records.map { record ->
                    MoneyUsageCategoryRepository.CategoryResult(
                        userId = UserId(record.userId!!),
                        moneyUsageCategoryId = MoneyUsageCategoryId(record.moneyUsageCategoryId!!),
                        name = record.name!!,
                    )
                }
            }
        }.fold(
            onSuccess = { MoneyUsageCategoryRepository.GetCategoryResult.Success(it) },
            onFailure = { MoneyUsageCategoryRepository.GetCategoryResult.Failed(it) },
        )
    }

    override fun updateCategory(
        userId: UserId,
        categoryId: MoneyUsageCategoryId,
        name: String?,
    ): Boolean {
        return DbConnectionImpl.use { connection ->
            if (name != null) {
                DSL.using(connection)
                    .update(CATEGORIES)
                    .set(CATEGORIES.NAME, name)
                    .where(
                        DSL.value(true)
                            .and(CATEGORIES.USER_ID.eq(userId.value))
                            .and(CATEGORIES.MONEY_USAGE_CATEGORY_ID.eq(categoryId.value)),
                    )
                    .limit(1)
                    .execute()
            } else {
                0
            } >= 1
        }
    }
}
