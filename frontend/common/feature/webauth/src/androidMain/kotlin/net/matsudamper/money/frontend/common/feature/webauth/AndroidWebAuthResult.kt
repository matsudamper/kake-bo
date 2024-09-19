package net.matsudamper.money.frontend.common.feature.webauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
internal data class AndroidWebAuthResult(
    @SerialName("id") val id: String? = null,
    @SerialName("rawId") val rawId: String? = null,
    @SerialName("authenticatorAttachment") val authenticatorAttachment: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("clientExtensionResults") val clientExtensionResults: Map<String, String>? = null,
    @SerialName("response") val response: Response,
) {
    @Serializable
    data class Response(
        @SerialName("clientDataJSON") val clientDataJSON: String? = null,
        @SerialName("authenticatorData") val authenticatorData: String? = null,
        @SerialName("signature") val signature: String? = null,
        @SerialName("userHandle") val userHandle: String? = null,
    )
}
