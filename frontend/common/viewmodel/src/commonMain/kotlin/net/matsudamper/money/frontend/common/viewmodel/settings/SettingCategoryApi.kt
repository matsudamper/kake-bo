package net.matsudamper.money.frontend.common.viewmodel.settings

import kotlinx.coroutines.flow.Flow
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.graphql.AddCategoryMutation
import net.matsudamper.money.frontend.graphql.CategorySettingScreenQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.SubCategorySettingScreenQuery
import net.matsudamper.money.frontend.graphql.type.AddCategoryInput
import net.matsudamper.money.frontend.graphql.type.MoneyUsageCategoriesInput
import net.matsudamper.money.frontend.graphql.type.MoneyUsageSubCategoriesFromCategoryIdQuery

public class SettingCategoryApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    public suspend fun getCategory(): ApolloResponse<CategorySettingScreenQuery.Data>? {
        return runCatching {
            apolloClient
                .query(
                    CategorySettingScreenQuery(
                        MoneyUsageCategoriesInput(
                            cursor = Optional.present(null),
                            size = 100,
                        ),
                    ),
                )
                .execute()
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

    public fun getSubCategory(
        id: MoneyUsageCategoryId,
    ): Flow<ApolloResponse<SubCategorySettingScreenQuery.Data>> {
        return apolloClient
            .query(
                SubCategorySettingScreenQuery(
                    MoneyUsageSubCategoriesFromCategoryIdQuery(
                        id = id,
                        cursor = Optional.present(null),
                        size = 100,
                    ),
                ),
            )
            .toFlow()
    }
}
