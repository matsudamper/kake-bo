package net.matsudamper.money.backend

import kotlinx.serialization.Serializable
import com.fasterxml.jackson.annotation.JsonProperty
import net.matsudamper.money.backend.base.CookieManager
import net.matsudamper.money.backend.base.mailparser.MailParser

class RegisterMailHandler(
    private val cookieManager: CookieManager,
) {
    fun handle(request: Request): Response {
        val userId = cookieManager.getUserSessionId()
        val result = MailParser.rawContentToResponse(request.raw)
        return Response(
            status =
                if (result.content.isEmpty()) {
                    Response.Status.ERROR
                } else {
                    Response.Status.OK
                },
        )
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
