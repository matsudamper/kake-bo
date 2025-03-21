package net.matsudamper.money.frontend.common.viewmodel.root.settings.api

import kotlinx.coroutines.flow.Flow
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.cache.normalized.watch
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
}
