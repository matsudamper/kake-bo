package net.matsudamper.money.frontend.common.viewmodel.login

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.LoginScreenFidoInfoQuery

public class LoginScreenApi(
    private val graphqlClient: GraphqlClient,
) {
    public suspend fun fidoLoginInfo(): ApolloResponse<LoginScreenFidoInfoQuery.Data> {
        return graphqlClient.apolloClient
            .query(
                LoginScreenFidoInfoQuery(),
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
    }
}
