package net.matsudamper.money.frontend.common.ui.layout.image

import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.intercept.Interceptor
import coil3.network.httpHeaders
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import kotlinx.coroutines.flow.firstOrNull
import net.matsudamper.money.frontend.common.feature.localstore.DataStores
import net.matsudamper.money.frontend.graphql.serverHost
import java.net.URI

private const val UserSessionIdKey = "user_session_id"

public fun initializeImageLoader(context: Context) {
    val appContext = context.applicationContext
    val sessionDataStore = DataStores.create(appContext).sessionDataStore

    SingletonImageLoader.setSafe { _ ->
        ImageLoader.Builder(appContext)
            .components {
                add(OkHttpNetworkFetcherFactory())
                add(
                    Interceptor { chain ->
                        val session = sessionDataStore.data.firstOrNull()
                        val host = session?.serverHost.orEmpty().ifEmpty { serverHost }
                        val userSessionId = session?.userSessionId.orEmpty()
                        val targetUrl = chain.request.data as? String
                        if (userSessionId.isBlank() || targetUrl == null || !isSameHost(targetUrl, host)) {
                            return@Interceptor chain.proceed()
                        }

                        val request = chain.request.newBuilder()
                            .httpHeaders(
                                chain.request.httpHeaders.newBuilder()
                                    .set("Cookie", "$UserSessionIdKey=$userSessionId")
                                    .build(),
                            )
                            .build()
                        chain.withRequest(request).proceed()
                    },
                )
            }
            .build()
    }
}

private fun isSameHost(
    url: String,
    host: String,
): Boolean {
    if (host.isBlank()) {
        return false
    }
    return runCatching {
        val targetUri = URI(url)
        val targetHost = targetUri.host ?: return@runCatching false
        val targetPort = targetUri.port.takeIf { it >= 0 }

        val expectedUri = URI("https://$host")
        val expectedHost = expectedUri.host ?: return@runCatching false
        val expectedPort = expectedUri.port.takeIf { it >= 0 }

        targetHost.equals(expectedHost, ignoreCase = true) &&
            (expectedPort == null || targetPort == expectedPort)
    }.getOrDefault(false)
}
