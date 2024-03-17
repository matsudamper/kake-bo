package net.matsudamper.money.frontend.common.viewmodel.root.settings.api

import kotlinx.coroutines.flow.Flow
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.cache.normalized.watch
import net.matsudamper.money.frontend.graphql.ApiSettingScreenQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient

public class ApiSettingScreenApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    public fun get(): Flow<ApolloResponse<ApiSettingScreenQuery.Data>> {
        return apolloClient
            .query(
                ApiSettingScreenQuery(),
            )
            .watch(
                fetchThrows = true,
                refetchThrows = false,
            )
    }
}