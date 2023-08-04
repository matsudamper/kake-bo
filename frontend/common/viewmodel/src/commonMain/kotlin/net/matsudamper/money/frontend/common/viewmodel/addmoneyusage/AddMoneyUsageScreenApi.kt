package net.matsudamper.money.frontend.common.viewmodel.addmoneyusage

import kotlinx.datetime.LocalDateTime
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.graphql.AddMoneyUsageMutation
import net.matsudamper.money.frontend.graphql.AddMoneyUsageScreenCategoriesPagingQuery
import net.matsudamper.money.frontend.graphql.AddMoneyUsageScreenSubCategoriesPagingQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.type.AddUsageQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsageCategoriesInput
import net.matsudamper.money.frontend.graphql.type.MoneyUsageSubCategoryQuery

public class AddMoneyUsageScreenApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    public suspend fun getCategories(): ApolloResponse<AddMoneyUsageScreenCategoriesPagingQuery.Data>? {
        return runCatching {
            apolloClient
                .query(
                    AddMoneyUsageScreenCategoriesPagingQuery(
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

    public suspend fun getSubCategoriesPaging(
        id: MoneyUsageCategoryId,
    ): ApolloResponse<AddMoneyUsageScreenSubCategoriesPagingQuery.Data>? {
        return runCatching {
            apolloClient
                .query(
                    AddMoneyUsageScreenSubCategoriesPagingQuery(
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

    public suspend fun addMoneyUsage(
        title: String,
        description: String,
        amount: Int,
        datetime: LocalDateTime,
        subCategoryId: MoneyUsageSubCategoryId?,
    ): ApolloResponse<AddMoneyUsageMutation.Data>? {
        return runCatching {
            apolloClient
                .mutation(
                    AddMoneyUsageMutation(
                        AddUsageQuery(
                            subCategoryId = Optional.present(subCategoryId),
                            title = title,
                            description = description,
                            amount = amount,
                            date = datetime,
                        ),
                    ),
                )
                .execute()
        }.getOrNull()
    }
}
