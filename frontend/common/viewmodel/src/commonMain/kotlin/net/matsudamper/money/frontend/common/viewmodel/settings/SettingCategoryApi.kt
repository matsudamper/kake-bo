package net.matsudamper.money.frontend.common.viewmodel.settings

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.graphql.AddCategoryMutation
import net.matsudamper.money.frontend.graphql.AddSubCategoryMutation
import net.matsudamper.money.frontend.graphql.CategoriesSettingScreenQuery
import net.matsudamper.money.frontend.graphql.CategorySettingScreenQuery
import net.matsudamper.money.frontend.graphql.CategorySettingScreenSubCategoriesPagingQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UpdateCategoryMutation
import net.matsudamper.money.frontend.graphql.type.AddCategoryInput
import net.matsudamper.money.frontend.graphql.type.AddSubCategoryInput
import net.matsudamper.money.frontend.graphql.type.MoneyUsageCategoriesInput
import net.matsudamper.money.frontend.graphql.type.MoneyUsageSubCategoryQuery
import net.matsudamper.money.frontend.graphql.type.UpdateCategoryQuery

public class SettingCategoryApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    public suspend fun getCategories(): ApolloResponse<CategoriesSettingScreenQuery.Data>? {
        return runCatching {
            apolloClient
                .query(
                    CategoriesSettingScreenQuery(
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

    public suspend fun addCategory(
        name: String,
    ): ApolloResponse<AddCategoryMutation.Data>? {
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

    public suspend fun getSubCategoriesPaging(
        id: MoneyUsageCategoryId,
    ): ApolloResponse<CategorySettingScreenSubCategoriesPagingQuery.Data>? {
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
        name: String,
    ): ApolloResponse<UpdateCategoryMutation.Data>? {
        return runCatching {
            apolloClient
                .mutation(
                    UpdateCategoryMutation(
                        id = id,
                        query = UpdateCategoryQuery(
                            name = Optional.present(name),
                        ),
                    ),
                )
                .execute()
        }.getOrNull()
    }

    public suspend fun getCategoryInfo(id: MoneyUsageCategoryId): ApolloResponse<CategorySettingScreenQuery.Data>? {
        return runCatching {
            apolloClient
                .query(
                    CategorySettingScreenQuery(
                        categoryId = id,
                    ),
                )
                .fetchPolicy(FetchPolicy.CacheAndNetwork)
                .execute()
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }
}
