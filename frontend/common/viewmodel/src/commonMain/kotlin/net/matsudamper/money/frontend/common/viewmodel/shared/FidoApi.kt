package net.matsudamper.money.frontend.common.viewmodel.shared

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
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
