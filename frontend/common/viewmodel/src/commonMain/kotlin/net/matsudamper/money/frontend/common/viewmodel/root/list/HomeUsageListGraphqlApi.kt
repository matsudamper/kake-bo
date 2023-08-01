package net.matsudamper.money.frontend.common.viewmodel.root.list

import kotlinx.coroutines.flow.Flow
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.HomeScreenQuery
import net.matsudamper.money.frontend.graphql.UsageListScreenPagingQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQuery
import org.jetbrains.skiko.Cursor

public class HomeUsageListGraphqlApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    public fun getHomeScreen(
        cursor: Cursor?,
    ): Flow<ApolloResponse<UsageListScreenPagingQuery.Data>> {
        return apolloClient
            .query(
                UsageListScreenPagingQuery(
                    query = MoneyUsagesQuery(
                        cursor = Optional.present(cursor),
                        size = 10,
                    )
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .toFlow()
    }
}
