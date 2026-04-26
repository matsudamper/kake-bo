package net.matsudamper.money.frontend.common.viewmodel.root.settings.login

import kotlinx.coroutines.flow.Flow
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.watch
import net.matsudamper.money.element.FidoId
import net.matsudamper.money.element.SessionRecordId
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.graphql.LoginSettingScreenChangeSessionNameMutation
import net.matsudamper.money.frontend.graphql.LoginSettingScreenDeleteSessionMutation
import net.matsudamper.money.frontend.graphql.LoginSettingScreenLogoutMutation
import net.matsudamper.money.frontend.graphql.LoginSettingScreenQuery
import net.matsudamper.money.frontend.graphql.SettingScreenAddFidoMutation
import net.matsudamper.money.frontend.graphql.SettingScreenDeleteFidoMutation
import net.matsudamper.money.frontend.graphql.type.RegisterFidoInput

private const val TAG = "LoginSettingScreenApi"

public class LoginSettingScreenApi(
    private val apolloClient: ApolloClient,
) {
    public fun getScreen(): Flow<ApolloResponse<LoginSettingScreenQuery.Data>> {
        return apolloClient
            .query(
                LoginSettingScreenQuery(),
            )
            .fetchPolicy(FetchPolicy.NetworkFirst)
            .watch()
    }

    public suspend fun logout(): Boolean {
        return runCatching {
            apolloClient
                .mutation(
                    LoginSettingScreenLogoutMutation(),
                ).execute().data?.userMutation?.logout
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull() ?: false
    }

    public suspend fun addFido(
        displayName: String,
        base64AttestationObject: String,
        base64ClientDataJson: String,
        challenge: String,
    ): ApolloResponse<SettingScreenAddFidoMutation.Data>? {
        return runCatching {
            apolloClient
                .mutation(
                    SettingScreenAddFidoMutation(
                        RegisterFidoInput(
                            displayName = displayName,
                            base64AttestationObject = base64AttestationObject,
                            base64ClientDataJson = base64ClientDataJson,
                            challenge = challenge,
                        ),
                    ),
                )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull()
    }

    public suspend fun deleteFido(id: FidoId): Boolean {
        return runCatching {
            apolloClient
                .mutation(
                    SettingScreenDeleteFidoMutation(id),
                )
                .execute()
                .data?.userMutation?.deleteFido?.isSuccess
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull() ?: false
    }

    public suspend fun deleteSession(id: SessionRecordId): Boolean {
        return runCatching {
            apolloClient
                .mutation(
                    LoginSettingScreenDeleteSessionMutation(id),
                )
                .execute()
                .data?.userMutation?.deleteSession?.isSuccess
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull() ?: false
    }

    public suspend fun changeSessionName(id: SessionRecordId, name: String): Boolean {
        return runCatching {
            apolloClient
                .mutation(
                    LoginSettingScreenChangeSessionNameMutation(id = id, name = name),
                )
                .execute()
                .data?.userMutation?.changeSessionName?.isSuccess
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull() ?: false
    }
}
