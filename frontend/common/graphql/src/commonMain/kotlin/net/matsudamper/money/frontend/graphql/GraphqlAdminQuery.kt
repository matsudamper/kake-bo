package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import net.matsudamper.money.element.ImageId

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

    suspend fun getUnlinkedImages(
        size: Int,
        cursor: String?,
    ): ApolloResponse<AdminUnlinkedImagesQuery.Data> {
        return graphqlClient.apolloClient
            .query(
                AdminUnlinkedImagesQuery(
                    size = size,
                    cursor = Optional.present(cursor),
                ),
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
    }

    suspend fun getUnlinkedImagesTotalCount(): ApolloResponse<AdminUnlinkedImagesTotalCountQuery.Data> {
        return graphqlClient.apolloClient
            .query(AdminUnlinkedImagesTotalCountQuery())
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
    }

    suspend fun getImageDirectoryMonths(): ApolloResponse<AdminImageDirectoryMonthsQuery.Data> {
        return graphqlClient.apolloClient
            .query(AdminImageDirectoryMonthsQuery())
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
    }

    suspend fun getUnlinkedImagesByMonth(yearMonth: String): ApolloResponse<AdminUnlinkedImagesByMonthQuery.Data> {
        return graphqlClient.apolloClient
            .query(AdminUnlinkedImagesByMonthQuery(yearMonth = yearMonth))
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
    }

    suspend fun deleteUnlinkedImages(imageIds: List<ImageId>): Boolean {
        return graphqlClient.apolloClient
            .mutation(AdminDeleteUnlinkedImagesMutation(imageIds = imageIds))
            .execute()
            .data?.adminMutation?.deleteUnlinkedImages == true
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

    public sealed interface IsLoggedInResult {
        public data object LoggedIn : IsLoggedInResult
        public data object NotLoggedIn : IsLoggedInResult
        public data object ServerError : IsLoggedInResult
    }
}
