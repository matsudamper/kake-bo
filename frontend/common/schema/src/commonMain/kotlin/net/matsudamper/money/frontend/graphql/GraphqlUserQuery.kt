package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo3.api.ApolloResponse

class GraphqlUserQuery {
    suspend fun login(
        userName: String,
        password: String,
    ): ApolloResponse<UserLoginMutation.Data> {
        return GraphqlClient.apolloClient
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
            GraphqlClient.apolloClient
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