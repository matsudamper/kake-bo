package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse

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
