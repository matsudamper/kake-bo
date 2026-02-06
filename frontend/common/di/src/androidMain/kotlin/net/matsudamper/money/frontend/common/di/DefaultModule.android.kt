package net.matsudamper.money.frontend.common.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.get
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.network.http.HttpInfo
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.feature.localstore.DataStores
import net.matsudamper.money.frontend.common.feature.webauth.WebAuthModel
import net.matsudamper.money.frontend.common.feature.webauth.WebAuthModelAndroidImpl
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.GraphqlClientImpl
import net.matsudamper.money.frontend.graphql.ServerHostConfig
import org.koin.core.scope.Scope

internal actual val factory: Factory = object : Factory() {
    private val UserSessionIdKey = "user_session_id"
    override fun createWebAuthModule(scope: Scope): WebAuthModel {
        return WebAuthModelAndroidImpl(
            context = scope.get(),
        )
    }

    override fun createGraphQlClient(scope: Scope): GraphqlClient {
        val sessionDataStore = scope.get<DataStores>().sessionDataStore
        val config = scope.get<ServerHostConfig>()
        val initialHost = config.savedHost.takeIf { it.isNotEmpty() }
            ?: config.defaultHost.takeIf { it.isNotEmpty() }
            .orEmpty()
        val initialServerUrl = if (initialHost.isNotEmpty()) {
            "${config.protocol}://$initialHost/query"
        } else {
            ""
        }

        var activeHost = initialHost

        return GraphqlClientImpl(
            serverUrl = initialServerUrl,
            interceptors = listOf(
                object : ApolloInterceptor {
                    override fun <D : Operation.Data> intercept(
                        request: ApolloRequest<D>,
                        chain: ApolloInterceptorChain,
                    ): Flow<ApolloResponse<D>> {
                        val userSessionId = runBlocking { sessionDataStore.data.firstOrNull() }?.userSessionId.orEmpty()
                        val response = chain.proceed(
                            request.newBuilder()
                                .addHttpHeader("Cookie", "$UserSessionIdKey=$userSessionId")
                                .build(),
                        )

                        return response.onEach { eachResponse ->
                            @OptIn(ApolloExperimental::class)
                            val cookies = eachResponse.executionContext[HttpInfo]?.headers
                                ?.get("Set-Cookie")
                                ?: return@onEach
                            for (cookie in cookies.split(";")) {
                                val key: String
                                val value: String
                                run {
                                    cookie.split("=").let {
                                        key = it[0]
                                        value = it[1]
                                    }
                                }
                                if (key != UserSessionIdKey) continue
                                sessionDataStore.updateData {
                                    it.toBuilder()
                                        .setUserSessionId(value)
                                        .setServerHost(activeHost)
                                        .build()
                                }
                            }
                        }
                    }
                },
                object : ApolloInterceptor {
                    override fun <D : Operation.Data> intercept(
                        request: ApolloRequest<D>,
                        chain: ApolloInterceptorChain,
                    ): Flow<ApolloResponse<D>> {
                        Logger.i("Graphql", "-->Operation: ${request.operation.name()}(${request.requestUuid})")

                        return chain.proceed(request).onEach {
                            it.executionContext[HttpInfo]?.let { httpInfo ->
                                Logger.i("Graphql", "<--Operation: ${request.operation.name()}(${request.requestUuid}) ${httpInfo.statusCode}")
                            }
                        }
                    }
                },
            ),
            onServerUrlChanged = { url ->
                activeHost = url.substringAfter("://").substringBefore("/")
            },
        )
    }
}
