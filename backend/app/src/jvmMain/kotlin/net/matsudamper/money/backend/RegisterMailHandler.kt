package net.matsudamper.money.backend

import kotlinx.serialization.Serializable
import com.fasterxml.jackson.annotation.JsonProperty
import net.matsudamper.money.backend.base.CookieManager

class RegisterMailHandler(
    private val cookieManager: CookieManager
) {
    fun handle(request: Request) : Response {
        TODO()
    }

    @Serializable
    data class Request(
        @JsonProperty("raw") val raw: String,
    )

    @Serializable
    data class Response(
        @JsonProperty("status") val status: Status,
    ) {
        @Serializable
        enum class Status {
            OK,
            ERROR,
        }
    }
}
