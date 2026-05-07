package net.matsudamper.money.backend.feature.oidc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/**
 * issuerにパスが含まれる場合（例: https://example.com/oidc）、
 * RFC8414に従ったDiscoveryエンドポイントは https://example.com/oidc/.well-known/openid-configuration だが、
 * 現状は /.well-known/openid-configuration（ルートパス）に固定しているためずれが生じる。
 * 現在の設計はパスなしのissuerを前提としている。
 */
public fun Route.oidcDiscovery(issuer: String) {
    val normalizedIssuer = issuer.removeSuffix("/")
    get("/.well-known/openid-configuration") {
        call.respond(
            OidcDiscoveryDocument(
                issuer = normalizedIssuer,
                jwksUri = "$normalizedIssuer/jwks",
                responseTypesSupported = listOf("id_token"),
                subjectTypesSupported = listOf("public"),
                idTokenSigningAlgValuesSupported = listOf("RS256"),
            ),
        )
    }
}

@Serializable
private data class OidcDiscoveryDocument(
    @SerialName("issuer") val issuer: String,
    @SerialName("jwks_uri") val jwksUri: String,
    @SerialName("response_types_supported") val responseTypesSupported: List<String>,
    @SerialName("subject_types_supported") val subjectTypesSupported: List<String>,
    @SerialName("id_token_signing_alg_values_supported") val idTokenSigningAlgValuesSupported: List<String>,
)
