package net.matsudamper.money.frontend.common.viewmodel.login

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.LoginScreenFidoInfoQuery

public class LoginScreenApi(
    private val graphqlClient: GraphqlClient,
) {
    public suspend fun fidoLoginInfo(userName: String): ApolloResponse<LoginScreenFidoInfoQuery.Data> {
        return graphqlClient.apolloClient
            .query(
                LoginScreenFidoInfoQuery(userName = userName),
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
    }
}
