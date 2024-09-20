package net.matsudamper.money.frontend.common.viewmodel.shared

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.LoginSettingScreenGetFidoInfoQuery

public class FidoApi(
    private val apolloClient: ApolloClient,
) {
    public suspend fun getFidoInfo(): Result<ApolloResponse<LoginSettingScreenGetFidoInfoQuery.Data>> {
        return runCatching {
            apolloClient
                .query(
                    LoginSettingScreenGetFidoInfoQuery(),
                )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }
    }
}
