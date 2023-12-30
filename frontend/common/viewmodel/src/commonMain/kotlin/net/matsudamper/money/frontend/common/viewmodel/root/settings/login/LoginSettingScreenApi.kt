package net.matsudamper.money.frontend.common.viewmodel.root.settings.login

import kotlinx.coroutines.flow.Flow
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.watch
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.LoginSettingScreenLogoutMutation
import net.matsudamper.money.frontend.graphql.LoginSettingScreenQuery
import net.matsudamper.money.frontend.graphql.SettingScreenAddFidoMutation
import net.matsudamper.money.frontend.graphql.type.RegisterFidoInput

public class LoginSettingScreenApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    public fun getScreen(): Flow<ApolloResponse<LoginSettingScreenQuery.Data>> {
        return apolloClient
            .query(
                LoginSettingScreenQuery(),
            )
            .watch()
    }

    public suspend fun logout(): Boolean {
        return runCatching {
            apolloClient
                .mutation(
                    LoginSettingScreenLogoutMutation(),
                ).execute().data?.userMutation?.logout
        }.getOrNull() ?: false
    }

    public suspend fun addFido(
        displayName: String,
        base64AttestationObject: String,
        base64ClientDataJson: String,
    ): ApolloResponse<SettingScreenAddFidoMutation.Data>? {
        return runCatching {
            apolloClient
                .mutation(
                    SettingScreenAddFidoMutation(
                        RegisterFidoInput(
                            displayName = displayName,
                            base64AttestationObject = base64AttestationObject,
                            base64ClientDataJson = base64ClientDataJson,
                        ),
                    ),
                )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }
}
