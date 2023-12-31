package net.matsudamper.money.frontend.common.viewmodel.login

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.LoginScreenFidoInfoQuery

public class LoginScreenApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    public suspend fun fidoLoginInfo(): ApolloResponse<LoginScreenFidoInfoQuery.Data> {
        return apolloClient.query(
            LoginScreenFidoInfoQuery(),
        ).execute()
    }
}
