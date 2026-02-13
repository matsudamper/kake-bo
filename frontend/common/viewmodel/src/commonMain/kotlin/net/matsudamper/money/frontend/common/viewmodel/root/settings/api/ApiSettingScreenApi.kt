package net.matsudamper.money.frontend.common.viewmodel.root.settings.api

import kotlinx.coroutines.flow.Flow
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.watch
import net.matsudamper.money.element.ApiTokenId
import net.matsudamper.money.frontend.graphql.ApiSettingScreenDeleteApiTokenMutation
import net.matsudamper.money.frontend.graphql.ApiSettingScreenQuery
import net.matsudamper.money.frontend.graphql.ApiSettingScreenRegisterApiTokenMutation

public class ApiSettingScreenApi(
    private val apolloClient: ApolloClient,
) {
    public fun get(): Flow<ApolloResponse<ApiSettingScreenQuery.Data>> {
        return apolloClient
            .query(
                ApiSettingScreenQuery(),
            )
            .watch()
    }

    public suspend fun updateCache() {
        apolloClient
            .query(
                ApiSettingScreenQuery(),
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
    }

    public suspend fun registerToken(name: String): Result<ApolloResponse<ApiSettingScreenRegisterApiTokenMutation.Data>> {
        return runCatching {
            apolloClient
                .mutation(
                    ApiSettingScreenRegisterApiTokenMutation(
                        name = name,
                    ),
                )
                .execute()
        }
    }

    public suspend fun deleteToken(id: ApiTokenId): Boolean {
        return runCatching {
            apolloClient.mutation(
                ApiSettingScreenDeleteApiTokenMutation(
                    id = id,
                ),
            ).execute()
        }.getOrNull()?.data?.userMutation?.deleteApiToken?.isSuccess == true
    }
}
