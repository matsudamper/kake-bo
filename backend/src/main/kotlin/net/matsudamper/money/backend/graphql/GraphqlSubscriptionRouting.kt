package net.matsudamper.money.backend.graphql

import java.time.OffsetDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
        val subscriptions = mutableMapOf<String, Job>()

        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val message = jacksonObjectMapper().readValue<ClientMessage>(frame.readText())
                when (message.type) {
                    "connection_init" -> send(serverMessage(type = "connection_ack"))

                    "ping" -> send(serverMessage(type = "pong"))

                    "subscribe" -> {
                        val id = message.id ?: continue
                        val payload = message.payload ?: continue
                        if (subscriptions.containsKey(id)) {
                            close(CloseReason(code = 4409, message = "Subscriber for $id already exists"))
                            return@webSocket
                        }
                        subscriptions[id] = launch {
                            runSubscription(id = id, payload = payload, handler = handler)
                        }.also { job ->
                            job.invokeOnCompletion { subscriptions.remove(id) }
                        }
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
    val message = buildMap<String, Any> {
        put("type", type)
        if (id != null) put("id", id)
        if (payload != null) put("payload", payload)
    }
    return Frame.Text(ObjectMapper.jackson.writeValueAsString(message))
}

private data class ClientMessage(
    val type: String,
    val id: String? = null,
    val payload: SubscribePayload? = null,
)

private data class SubscribePayload(
    val query: String,
    val operationName: String? = null,
    val variables: Map<String, Any?>? = null,
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
