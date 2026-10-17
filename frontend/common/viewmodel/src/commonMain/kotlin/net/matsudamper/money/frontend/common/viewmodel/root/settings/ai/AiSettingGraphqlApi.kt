package net.matsudamper.money.frontend.common.viewmodel.root.settings.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.cache.normalized.FetchPolicy
import com.apollographql.cache.normalized.fetchPolicy
import net.matsudamper.money.frontend.common.base.PlatformIO
import net.matsudamper.money.frontend.graphql.AiSettingScreenQuery
import net.matsudamper.money.frontend.graphql.SetGeminiApiKeyMutation

public class AiSettingGraphqlApi(
    private val apolloClient: ApolloClient,
) {
    public suspend fun getAiConfig(): ApolloResponse<AiSettingScreenQuery.Data> {
        return withContext(Dispatchers.PlatformIO) {
            apolloClient
                .query(AiSettingScreenQuery())
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }
    }

    public suspend fun setGeminiApiKey(apiKey: String): ApolloResponse<SetGeminiApiKeyMutation.Data> {
        return withContext(Dispatchers.PlatformIO) {
            apolloClient
                .mutation(SetGeminiApiKeyMutation(apiKey = Optional.present(apiKey)))
                .execute()
        }
    }
}
