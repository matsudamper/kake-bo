package net.matsudamper.money.backend.graphql

import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.routing.Route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import net.matsudamper.money.backend.GraphqlSubscriptionHandler
import net.matsudamper.money.backend.base.CookieManager
import net.matsudamper.money.backend.base.ObjectMapper
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.backend.feature.session.KtorCookieManager

/**
 * graphql-transport-ws プロトコルで Subscription を流す。
 * 購読ごとにコルーチンを持ち、クライアントの complete か切断でキャンセルする。
 */
internal fun Route.graphqlSubscription(diContainer: DiContainer) {
    webSocket(path = "/subscriptions", protocol = "graphql-transport-ws") {
        val handler = GraphqlSubscriptionHandler(
            cookieManager = WebSocketCookieManager(KtorCookieManager(call = call)),
            diContainer = diContainer,
        )
        val subscriptions = ConcurrentHashMap<String, Job>()

        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val message = ObjectMapper.jackson.readValue<ClientMessage>(frame.readText())
                when (message.type) {
                    "connection_init" -> send(serverMessage(type = "connection_ack"))

                    "ping" -> send(serverMessage(type = "pong"))

                    "subscribe" -> {
                        val id = message.id ?: continue
                        val payload = message.payload?.let { ObjectMapper.jackson.convertValue<SubscribePayload>(it) } ?: continue
                        if (subscriptions.containsKey(id)) {
                            close(CloseReason(code = 4409, message = "Subscriber for $id already exists"))
                            return@webSocket
                        }
                        val job = launch(start = CoroutineStart.LAZY) {
                            runSubscription(id = id, payload = payload, handler = handler)
                        }
                        subscriptions[id] = job
                        job.invokeOnCompletion { subscriptions.remove(id, job) }
                        job.start()
                    }

                    "complete" -> {
                        val id = message.id ?: continue
                        subscriptions.remove(id)?.cancelAndJoin()
                    }
                }
            }
        } finally {
            subscriptions.values.forEach { it.cancel() }
        }
    }
}

private suspend fun DefaultWebSocketServerSession.runSubscription(
    id: String,
    payload: SubscribePayload,
    handler: GraphqlSubscriptionHandler,
) {
    val result = handler.subscribe(
        query = payload.query,
        operationName = payload.operationName,
        variables = payload.variables.orEmpty(),
    )
    when (result) {
        is GraphqlSubscriptionHandler.Result.Error -> {
            send(serverMessage(type = "error", id = id, payload = result.errors))
        }

        is GraphqlSubscriptionHandler.Result.Stream -> {
            try {
                result.events
                    .onCompletion { cause ->
                        if (cause == null) send(serverMessage(type = "complete", id = id))
                    }
                    .collect { event ->
                        send(serverMessage(type = "next", id = id, payload = event))
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                TraceLogger.impl().noticeThrowable(e, true)
                send(serverMessage(type = "error", id = id, payload = listOf(mapOf("message" to "InternalServerError"))))
            }
        }
    }
}

private fun serverMessage(
    type: String,
    id: String? = null,
    payload: Any? = null,
): Frame.Text {
    val message: Map<String, Any> = buildMap {
        put("type", type)
        if (id != null) put("id", id)
        if (payload != null) put("payload", payload)
    }
    return Frame.Text(ObjectMapper.jackson.writeValueAsString(message))
}

/**
 * payload は connection_init や ping では任意の JSON なので、subscribe のときだけ [SubscribePayload] に変換する
 */
@JsonIgnoreProperties(ignoreUnknown = true)
private data class ClientMessage(
    @param:JsonProperty("type") val type: String,
    @param:JsonProperty("id") val id: String? = null,
    @param:JsonProperty("payload") val payload: Map<String, Any?>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class SubscribePayload(
    @param:JsonProperty("query") val query: String,
    @param:JsonProperty("operationName") val operationName: String? = null,
    @param:JsonProperty("variables") val variables: Map<String, Any?>? = null,
)

/**
 * WebSocket はアップグレード後に応答ヘッダを変えられないので、Cookie の読み取りだけを通す
 */
private class WebSocketCookieManager(
    private val delegate: CookieManager,
) : CookieManager {
    override fun getAdminSessionId(): String? = delegate.getAdminSessionId()

    override fun getUserSessionId(): String? = delegate.getUserSessionId()

    override fun setAdminSession(idValue: String, expires: OffsetDateTime) = Unit

    override fun setUserSession(idValue: String, expires: OffsetDateTime) = Unit

    override fun clearAdminSession() = Unit

    override fun clearUserSession() = Unit
}
