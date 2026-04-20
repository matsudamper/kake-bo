package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo.api.ApolloResponse

class GraphqlAdminQuery(
    private val graphqlClient: GraphqlClient,
) {
    suspend fun adminLogin(password: String): ApolloResponse<AdminLoginMutation.Data> {
        return graphqlClient.apolloClient
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
        return graphqlClient.apolloClient.mutation(
            AdminAddUserMutation(
                userName = userName,
                password = password,
            ),
        )
            .execute()
    }

    suspend fun searchUsers(query: String): ApolloResponse<AdminSearchUsersMutation.Data> {
        return graphqlClient.apolloClient.mutation(
            AdminSearchUsersMutation(
                query = query,
            ),
        )
            .execute()
    }

    suspend fun resetPassword(
        userName: String,
        password: String,
    ): ApolloResponse<AdminResetPasswordMutation.Data> {
        return graphqlClient.apolloClient.mutation(
            AdminResetPasswordMutation(
                userName = userName,
                password = password,
            ),
        )
            .execute()
    }
}
