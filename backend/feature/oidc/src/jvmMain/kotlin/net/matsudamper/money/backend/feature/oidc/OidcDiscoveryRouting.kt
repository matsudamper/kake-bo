package net.matsudamper.money.backend.feature.oidc

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

public fun Route.oidcDiscovery(issuer: String) {
    get("/.well-known/openid-configuration") {
        call.respond(
            mapOf(
                "issuer" to issuer,
                "jwks_uri" to "$issuer/jwks",
                "response_types_supported" to listOf("id_token"),
                "subject_types_supported" to listOf("public"),
                "id_token_signing_alg_values_supported" to listOf("RS256"),
            )
        )
    }
}
