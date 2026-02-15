package net.matsudamper.money

import android.content.Context
import androidx.datastore.core.DataStore
import coil3.EventListener
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.intercept.Interceptor
import coil3.network.httpHeaders
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import java.net.URI
import kotlinx.coroutines.flow.firstOrNull
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.feature.localstore.generated.Session
import net.matsudamper.money.frontend.graphql.serverHost

private const val UserSessionIdKey = "user_session_id"
private const val ImageLoaderLogTag = "ImageLoader"

internal fun initializeImageLoader(
    context: Context,
    sessionDataStore: DataStore<Session>,
) {
    SingletonImageLoader.setSafe { _ ->
        ImageLoader.Builder(context)
            .eventListenerFactory {
                object : EventListener() {
                    override fun onStart(request: ImageRequest) {
                        Logger.i(ImageLoaderLogTag, "--> load image: ${request.data}")
                    }

                    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                        Logger.i(ImageLoaderLogTag, "<-- load success: ${request.data} (source=${result.dataSource})")
                    }

                    override fun onError(request: ImageRequest, result: ErrorResult) {
                        Logger.e(
                            ImageLoaderLogTag,
                            "<-- load failed: ${request.data} (error=${result.throwable.message ?: result.throwable::class.simpleName})",
                        )
                    }
                }
            }
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
