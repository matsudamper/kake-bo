package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy

class GraphqlUserLoginQuery(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    suspend fun login(
        userName: String,
        password: String,
    ): ApolloResponse<UserLoginMutation.Data> {
        return apolloClient
            .mutation(
                UserLoginMutation(
                    userName = userName,
                    password = password,
                ),
            )
            .execute()
    }

    suspend fun isLoggedIn(): Boolean {
        return runCatching {
            apolloClient
                .query(UserIsLoggedInQuery())
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
                .data
                ?.user
                ?.isLoggedIn
        }.fold(
            onSuccess = { it == true },
            onFailure = { false },
        )
    }
}
