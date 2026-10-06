package net.matsudamper.money.frontend.graphql

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import okio.Buffer

private val DEFAULT_OPERATION_TIMEOUT = 5.seconds

private class OperationTimeout(val timeout: Duration) : ExecutionContext.Element {
    override val key: ExecutionContext.Key<*> = Key

    companion object Key : ExecutionContext.Key<OperationTimeout>
}

/**
 * watch() の Flow は ApolloInterceptor の内側で続くため、ここでは Flow にタイムアウトを掛けず、
 * 操作のタイムアウトを [OperationTimeoutHttpInterceptor] へ渡すだけにする。
 */
internal object OperationTimeoutInterceptor : ApolloInterceptor {
    override fun <D : Operation.Data> intercept(
        request: ApolloRequest<D>,
        chain: ApolloInterceptorChain,
    ): Flow<ApolloResponse<D>> {
        val operation = request.operation
        val timeout = if (operation is LongRunningOperation) {
            operation.timeoutSeconds.seconds
        } else {
            DEFAULT_OPERATION_TIMEOUT
        }
        return chain.proceed(
            request.newBuilder()
                .addExecutionContext(OperationTimeout(timeout))
                .build(),
        )
    }
}

internal object OperationTimeoutHttpInterceptor : HttpInterceptor {
    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain,
    ): HttpResponse {
        val timeout = request.executionContext[OperationTimeout]?.timeout ?: DEFAULT_OPERATION_TIMEOUT
        // HttpNetworkTransport は CancellationException をそのまま投げ直すため、通信エラーとして扱われる例外に変換する
        return withTimeoutOrNull(timeout) { chain.proceed(request).withBufferedBody() }
            ?: throw ApolloNetworkException(message = "Timeout: $timeout")
    }

    /**
     * 本文は返却後に遅延して読まれるため、タイムアウトの内側で読み切る。
     * 本文の読み取りはブロッキングでキャンセルに反応しないため、キャンセル時は本文を閉じて読み取りを中断させる。
     */
    private suspend fun HttpResponse.withBufferedBody(): HttpResponse {
        val body = body ?: return this
        val bufferedBody = coroutineScope {
            val bodyCloser = launch {
                try {
                    awaitCancellation()
                } finally {
                    body.close()
                }
            }
            try {
                withContext(Dispatchers.Default) { Buffer().apply { writeAll(body) } }
            } finally {
                bodyCloser.cancel()
            }
        }
        return HttpResponse.Builder(statusCode = statusCode)
            .headers(headers)
            .body(bufferedBody)
            .build()
    }
}
