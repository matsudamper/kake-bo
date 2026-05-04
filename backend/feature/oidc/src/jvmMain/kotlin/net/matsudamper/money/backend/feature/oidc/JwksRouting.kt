package net.matsudamper.money.backend.feature.oidc

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

public fun Route.jwks(keyManager: OidcKeyManager) {
    get("/jwks") {
        call.respondText(
            text = keyManager.publicJwkSet(),
            contentType = ContentType.Application.Json
        )
    }
}
