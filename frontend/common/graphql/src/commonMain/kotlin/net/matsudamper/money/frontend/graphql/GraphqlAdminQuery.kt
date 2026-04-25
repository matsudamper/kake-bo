package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.UserId

class GraphqlAdminQuery(
    private val graphqlClient: GraphqlClient,
) {
    suspend fun isLoggedIn(): IsLoggedInResult {
        val response = try {
            graphqlClient.apolloClient
                .query(AdminIsLoggedInQuery())
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            e.printStackTrace()
            return IsLoggedInResult.ServerError
        }

        val isLoggedIn = response.data?.isAdminLoggedIn
            ?: return IsLoggedInResult.ServerError

        return if (isLoggedIn) {
            IsLoggedInResult.LoggedIn
        } else {
            IsLoggedInResult.NotLoggedIn
        }
    }

    suspend fun adminLogin(password: String): ApolloResponse<AdminLoginMutation.Data> {
        return graphqlClient.apolloClient
            .mutation(
                AdminLoginMutation(
                    password = password,
                ),
            )
            .execute()
    }

    suspend fun adminLogout(): Boolean {
        return graphqlClient.apolloClient
            .mutation(AdminLogoutMutation())
            .execute()
            .data?.adminMutation?.adminLogout == true
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

    suspend fun deleteImages(
        imageIds: List<ImageId>,
    ): Boolean {
        return graphqlClient.apolloClient
            .mutation(AdminDeleteImagesMutation(imageIds = imageIds))
            .execute()
            .data?.adminMutation?.deleteImages == true
    }

    suspend fun searchUsers(
        query: String,
        size: Int,
        cursor: String?,
    ): ApolloResponse<AdminSearchUsersQuery.Data> {
        return graphqlClient.apolloClient.query(
            AdminSearchUsersQuery(
                query = query,
                size = size,
                cursor = com.apollographql.apollo.api.Optional.presentIfNotNull(cursor),
            ),
        )
            .execute()
    }

    suspend fun replacePassword(
        userId: UserId,
        password: String,
    ): ApolloResponse<AdminReplacePasswordMutation.Data> {
        return graphqlClient.apolloClient.mutation(
            AdminReplacePasswordMutation(
                userId = userId,
                password = password,
            ),
        )
            .execute()
    }

    public sealed interface IsLoggedInResult {
        public data object LoggedIn : IsLoggedInResult
        public data object NotLoggedIn : IsLoggedInResult
        public data object ServerError : IsLoggedInResult
    }
}
