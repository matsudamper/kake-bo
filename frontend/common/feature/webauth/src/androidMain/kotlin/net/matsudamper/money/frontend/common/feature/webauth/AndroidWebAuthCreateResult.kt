package net.matsudamper.money.frontend.common.feature.webauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AndroidWebAuthCreateResult(
    @SerialName("id") val id: String? = null,
    @SerialName("rawId") val rawId: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("response") val response: Response,
) {
    @Serializable
    data class Response(
        @SerialName("attestationObject") val attestationObject: String? = null,
        @SerialName("clientDataJSON") val clientDataJSON: String? = null,
    )
}
