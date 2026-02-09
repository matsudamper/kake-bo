package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import net.matsudamper.money.frontend.graphql.type.UserFidoLoginInput

class GraphqlUserLoginQuery(
    private val graphqlClient: GraphqlClient,
) {
    suspend fun login(
        userName: String,
        password: String,
    ): ApolloResponse<UserLoginMutation.Data> {
        return graphqlClient.apolloClient
            .mutation(
                UserLoginMutation(
                    userName = userName,
                    password = password,
                ),
            )
            .execute()
    }

    suspend fun webAuthLogin(input: UserFidoLoginInput): ApolloResponse<UserWebAuthnLoginMutation.Data> {
        return graphqlClient.apolloClient
            .mutation(
                UserWebAuthnLoginMutation(input = input),
            )
            .execute()
    }

    suspend fun isLoggedIn(): IsLoggedInResult {
        val response = try {
            graphqlClient.apolloClient
                .query(UserIsLoggedInQuery())
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
            return IsLoggedInResult.LoggedIn
        }

        val isLoggedIn = response.data?.isLoggedIn
            ?: return IsLoggedInResult.ServerError

        return if (isLoggedIn) {
            IsLoggedInResult.LoggedIn
        } else {
            IsLoggedInResult.NotLoggedIn
        }
    }

    public sealed interface IsLoggedInResult {
        public data object LoggedIn : IsLoggedInResult
        public data object NotLoggedIn : IsLoggedInResult
        public data object ServerError : IsLoggedInResult
    }
}
