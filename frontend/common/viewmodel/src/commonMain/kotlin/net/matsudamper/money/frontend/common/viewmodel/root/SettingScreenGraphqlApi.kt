package net.matsudamper.money.frontend.common.viewmodel.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.cache.normalized.FetchPolicy
import com.apollographql.cache.normalized.fetchPolicy
import net.matsudamper.money.frontend.common.base.PlatformIO
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.SettingRootScreenAdminLoginMutation
import net.matsudamper.money.frontend.graphql.SettingRootScreenQuery

public class SettingScreenGraphqlApi(
    private val graphqlClient: GraphqlClient,
) {
    public suspend fun getSettingRootScreen(): ApolloResponse<SettingRootScreenQuery.Data> {
        return withContext(Dispatchers.PlatformIO) {
            graphqlClient.apolloClient
                .query(SettingRootScreenQuery())
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }
    }

    public suspend fun adminLoginWithUserSession(): ApolloResponse<SettingRootScreenAdminLoginMutation.Data> {
        return withContext(Dispatchers.PlatformIO) {
            graphqlClient.apolloClient
                .mutation(SettingRootScreenAdminLoginMutation())
                .execute()
        }
    }
}
