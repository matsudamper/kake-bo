package net.matsudamper.money.frontend.common.viewmodel.settings

import kotlinx.coroutines.flow.Flow
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.watch
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.graphql.AddCategoryMutation
import net.matsudamper.money.frontend.graphql.AddSubCategoryMutation
import net.matsudamper.money.frontend.graphql.CategoriesSettingScreenCategoriesPagingQuery
import net.matsudamper.money.frontend.graphql.CategorySettingScreenQuery
import net.matsudamper.money.frontend.graphql.CategorySettingScreenSubCategoriesPagingQuery
import net.matsudamper.money.frontend.graphql.DeleteCategoryMutation
import net.matsudamper.money.frontend.graphql.DeleteSubCategoryMutation
import net.matsudamper.money.frontend.graphql.UpdateCategoryMutation
import net.matsudamper.money.frontend.graphql.UpdateSubCategoryMutation
import net.matsudamper.money.frontend.graphql.type.AddCategoryInput
import net.matsudamper.money.frontend.graphql.type.AddSubCategoryInput
import net.matsudamper.money.frontend.graphql.type.MoneyUsageCategoriesInput
import net.matsudamper.money.frontend.graphql.type.MoneyUsageSubCategoryQuery
import net.matsudamper.money.frontend.graphql.type.UpdateCategoryQuery
import net.matsudamper.money.frontend.graphql.type.UpdateSubCategoryQuery
import net.matsudamper.money.frontend.graphql.updateOperation

private const val TAG = "SettingScreenCategoryApi"

public class SettingScreenCategoryApi(
    private val apolloClient: ApolloClient,
) {
    public suspend fun getCategories(): ApolloResponse<CategoriesSettingScreenCategoriesPagingQuery.Data>? {
        return runCatching {
            apolloClient
                .query(
                    CategoriesSettingScreenCategoriesPagingQuery(
                        MoneyUsageCategoriesInput(
                            cursor = Optional.present(null),
                            size = 100,
                        ),
                    ),
                )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull()
    }

    public suspend fun addCategory(name: String): ApolloResponse<AddCategoryMutation.Data>? {
        return runCatching {
            apolloClient
                .mutation(
                    AddCategoryMutation(
                        category = AddCategoryInput(
                            name = name,
                        ),
                    ),
                )
                .execute()
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull()
    }

    public suspend fun addSubCategory(
        categoryId: MoneyUsageCategoryId,
        name: String,
    ): ApolloResponse<AddSubCategoryMutation.Data>? {
        return runCatching {
            apolloClient
                .mutation(
                    AddSubCategoryMutation(
                        category = AddSubCategoryInput(
                            categoryId = categoryId,
                            name = name,
                        ),
                    ),
                )
                .execute()
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull()
    }

    public fun getSubCategoriesPaging(id: MoneyUsageCategoryId): Flow<ApolloResponse<CategorySettingScreenSubCategoriesPagingQuery.Data>> {
        return apolloClient
            .query(createSubCategoriesPagingQuery(id))
            .fetchPolicy(FetchPolicy.CacheAndNetwork)
            .watch()
    }

    public suspend fun refetchSubCategoriesPaging(id: MoneyUsageCategoryId) {
        runCatching {
            fetchSubCategoriesPaging(
                query = createSubCategoriesPagingQuery(id),
                fetchPolicy = FetchPolicy.NetworkOnly,
            )
        }.onFailure {
            Logger.e(TAG, it)
        }
    }

    public suspend fun updateCategory(
        id: MoneyUsageCategoryId,
        name: Optional<String?>,
        color: Optional<String?>,
    ): ApolloResponse<UpdateCategoryMutation.Data>? {
        return runCatching {
            apolloClient
                .mutation(
                    UpdateCategoryMutation(
                        id = id,
                        query = UpdateCategoryQuery(
                            name = name,
                            color = color,
                        ),
                    ),
                )
                .execute()
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull()
    }

    public fun getCategoryInfo(id: MoneyUsageCategoryId): Flow<ApolloResponse<CategorySettingScreenQuery.Data>> {
        return apolloClient
            .query(
                CategorySettingScreenQuery(
                    categoryId = id,
                ),
            )
            .fetchPolicy(FetchPolicy.CacheAndNetwork)
            .watch()
    }

    public suspend fun updateSubCategory(
        id: MoneyUsageSubCategoryId,
        name: String,
    ): ApolloResponse<UpdateSubCategoryMutation.Data>? {
        return runCatching {
            apolloClient
                .mutation(
                    UpdateSubCategoryMutation(
                        id = id,
                        query = UpdateSubCategoryQuery(
                            name = Optional.present(name),
                        ),
                    ),
                )
                .execute()
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull()
    }

    public suspend fun deleteSubCategory(
        categoryId: MoneyUsageCategoryId,
        id: MoneyUsageSubCategoryId,
    ): Boolean {
        val isDeleted = runCatching {
            apolloClient
                .mutation(
                    DeleteSubCategoryMutation(
                        id = id,
                    ),
                )
                .execute()
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull()?.data?.userMutation?.deleteSubCategory == true
        if (!isDeleted) {
            return false
        }

        val cacheQuery = createSubCategoriesPagingQuery(categoryId)
        val updateResult = runCatching {
            apolloClient.updateOperation(cacheQuery) update@{ before ->
                if (before == null) {
                    return@update success(
                        fetchSubCategoriesPaging(
                            query = cacheQuery,
                            fetchPolicy = FetchPolicy.NetworkOnly,
                        ),
                    )
                }

                val user = before.user ?: return@update error()
                val moneyUsageCategory = user.moneyUsageCategory ?: return@update error()
                val subCategories = moneyUsageCategory.subCategories ?: return@update error()
                val response = fetchSubCategoriesPaging(
                    query = cacheQuery,
                    fetchPolicy = FetchPolicy.CacheOnly,
                )

                success(
                    response.newBuilder()
                        .data(
                            before.copy(
                                user = user.copy(
                                    moneyUsageCategory = moneyUsageCategory.copy(
                                        subCategories = subCategories.copy(
                                            nodes = subCategories.nodes.filterNot { it.id == id },
                                        ),
                                    ),
                                ),
                            ),
                        )
                        .build(),
                )
            }
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull()

        if (updateResult?.isSuccess() != true) {
            refetchSubCategoriesPaging(id = categoryId)
        }

        return true
    }

    public suspend fun deleteCategory(id: MoneyUsageCategoryId): Boolean {
        return runCatching {
            apolloClient
                .mutation(
                    DeleteCategoryMutation(
                        id = id,
                    ),
                )
                .execute()
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull()?.data?.userMutation?.deleteCategory == true
    }

    private fun createSubCategoriesPagingQuery(
        id: MoneyUsageCategoryId,
    ): CategorySettingScreenSubCategoriesPagingQuery {
        return CategorySettingScreenSubCategoriesPagingQuery(
            categoryId = id,
            query = MoneyUsageSubCategoryQuery(
                cursor = Optional.present(null),
                size = 100,
            ),
        )
    }

    private suspend fun fetchSubCategoriesPaging(
        query: CategorySettingScreenSubCategoriesPagingQuery,
        fetchPolicy: FetchPolicy,
    ): ApolloResponse<CategorySettingScreenSubCategoriesPagingQuery.Data> {
        return apolloClient
            .query(query)
            .fetchPolicy(fetchPolicy)
            .execute()
    }
}
