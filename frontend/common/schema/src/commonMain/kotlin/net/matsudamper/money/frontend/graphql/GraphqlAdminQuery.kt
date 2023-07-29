package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo3.api.ApolloResponse

class GraphqlAdminQuery {
    suspend fun adminLogin(password: String): ApolloResponse<AdminLoginMutation.Data> {
        return GraphqlClient.apolloClient
            .mutation(
                AdminLoginMutation(
                    password = password,
                ),
            )
            .execute()
    }

    suspend fun addUser(
        userName: String,
        password: String,
    ): ApolloResponse<AdminAddUserMutation.Data> {
        return GraphqlClient.apolloClient.mutation(
            AdminAddUserMutation(
                userName = userName,
                password = password,
            ),
        )
            .execute()
    }
}
