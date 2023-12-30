package net.matsudamper.money.frontend.common.viewmodel.shared

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.LoginSettingScreenGetFidoInfoQuery

public class FidoApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    public suspend fun getFidoInfo(): Result<ApolloResponse<LoginSettingScreenGetFidoInfoQuery.Data>> {
        return runCatching {
            apolloClient.query(
                LoginSettingScreenGetFidoInfoQuery(),
            ).execute()
        }
    }
}
