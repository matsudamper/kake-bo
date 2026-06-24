package net.matsudamper.money.frontend.common.viewmodel.settings

import kotlinx.coroutines.flow.Flow
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.apolloStore
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.watch
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.graphql.CategoriesSettingScreenCategoriesPagingQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.type.MoneyUsageCategoriesInput

private const val TAG = "SettingCategoriesScreenPagingModel"

public class SettingCategoriesScreenPagingModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val graphqlClient: GraphqlClient,
) : CommonViewModel(scopedObjectFeature) {

    private val firstQuery = CategoriesSettingScreenCategoriesPagingQuery(
        MoneyUsageCategoriesInput(
            cursor = Optional.present(null),
            size = 100,
        ),
    )

    internal fun getFlow(): Flow<ApolloResponse<CategoriesSettingScreenCategoriesPagingQuery.Data>> {
        return graphqlClient.apolloClient.query(firstQuery)
            .fetchPolicy(FetchPolicy.CacheAndNetwork)
            .watch()
    }

    internal suspend fun refetch() {
        runCatching {
            graphqlClient.apolloClient.query(firstQuery)
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }.onFailure {
            Logger.e(TAG, it)
        }
    }

    public suspend fun removeCategoryFromCache(categoryId: MoneyUsageCategoryId) {
        val apolloClient = graphqlClient.apolloClient
        val before = apolloClient.query(firstQuery)
            .fetchPolicy(FetchPolicy.CacheOnly)
            .execute()
            .data ?: return
        val connection = before.user?.moneyUsageCategories ?: return
        val filteredNodes = connection.nodes.filterNot { node ->
            node.id == categoryId
        }
        if (filteredNodes.size == connection.nodes.size) return
        apolloClient.apolloStore.writeOperation(
            operation = firstQuery,
            operationData = before.copy(
                user = before.user?.copy(
                    moneyUsageCategories = connection.copy(
                        nodes = filteredNodes,
                    ),
                ),
            ),
            customScalarAdapters = apolloClient.customScalarAdapters,
            publish = true,
        )
    }
}
