package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.flow.Flow
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenQuery

public class RootHomeTabScreenApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    public fun getHomeScreen(): Flow<ApolloResponse<RootHomeTabScreenQuery.Data>> {
        return apolloClient
            .query(RootHomeTabScreenQuery())
            .toFlow()
    }
}