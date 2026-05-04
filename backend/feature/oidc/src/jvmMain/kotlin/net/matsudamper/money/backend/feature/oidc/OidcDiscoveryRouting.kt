package net.matsudamper.money.backend.feature.oidc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

public fun Route.oidcDiscovery(issuer: String) {
    get("/.well-known/openid-configuration") {
        call.respond(
            OidcDiscoveryDocument(
                issuer = issuer,
                jwksUri = "$issuer/jwks",
                responseTypesSupported = listOf("id_token"),
                subjectTypesSupported = listOf("public"),
                idTokenSigningAlgValuesSupported = listOf("RS256"),
            ),
        )
    }
}

@Serializable
private data class OidcDiscoveryDocument(
    val issuer: String,
    @SerialName("jwks_uri") val jwksUri: String,
    @SerialName("response_types_supported") val responseTypesSupported: List<String>,
    @SerialName("subject_types_supported") val subjectTypesSupported: List<String>,
    @SerialName("id_token_signing_alg_values_supported") val idTokenSigningAlgValuesSupported: List<String>,
)
