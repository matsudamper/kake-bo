package net.matsudamper.money.frontend.common.viewmodel.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.watch
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.graphql.AddCategoryMutation
import net.matsudamper.money.frontend.graphql.AddSubCategoryMutation
import net.matsudamper.money.frontend.graphql.CategoriesSettingScreenCategoriesPagingQuery
import net.matsudamper.money.frontend.graphql.CategorySettingScreenQuery
import net.matsudamper.money.frontend.graphql.CategorySettingScreenSubCategoriesPagingQuery
import net.matsudamper.money.frontend.graphql.DeleteSubCategoryMutation
import net.matsudamper.money.frontend.graphql.UpdateCategoryMutation
import net.matsudamper.money.frontend.graphql.UpdateSubCategoryMutation
import net.matsudamper.money.frontend.graphql.type.AddCategoryInput
import net.matsudamper.money.frontend.graphql.type.AddSubCategoryInput
import net.matsudamper.money.frontend.graphql.type.MoneyUsageCategoriesInput
import net.matsudamper.money.frontend.graphql.type.MoneyUsageSubCategoryQuery
import net.matsudamper.money.frontend.graphql.type.UpdateCategoryQuery
import net.matsudamper.money.frontend.graphql.type.UpdateSubCategoryQuery

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
            it.printStackTrace()
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
        }.getOrNull()
    }

    public suspend fun getSubCategoriesPaging(id: MoneyUsageCategoryId): ApolloResponse<CategorySettingScreenSubCategoriesPagingQuery.Data>? {
        return runCatching {
            apolloClient
                .query(
                    CategorySettingScreenSubCategoriesPagingQuery(
                        categoryId = id,
                        query = MoneyUsageSubCategoryQuery(
                            cursor = Optional.present(null),
                            size = 100,
                        ),
                    ),
                )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
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
            .catch {
                it.printStackTrace()
            }
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
        }.getOrNull()
    }

    public suspend fun deleteSubCategory(id: MoneyUsageSubCategoryId): Boolean {
        return runCatching {
            apolloClient
                .mutation(
                    DeleteSubCategoryMutation(
                        id = id,
                    ),
                )
                .execute()
        }.getOrNull()?.data?.userMutation != null
    }
}
