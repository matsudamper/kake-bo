package net.matsudamper.money.frontend.common.viewmodel.login

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.LoginScreenChangeSessionNameMutation
import net.matsudamper.money.frontend.graphql.LoginScreenFidoInfoQuery

private const val TAG = "LoginScreenApi"

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

    public suspend fun changeSessionName(name: String): Boolean {
        return runCatching {
            graphqlClient.apolloClient
                .mutation(
                    LoginScreenChangeSessionNameMutation(name),
                )
                .execute()
                .data?.userMutation?.changeSessionName?.isSuccess
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull() ?: false
    }
}
