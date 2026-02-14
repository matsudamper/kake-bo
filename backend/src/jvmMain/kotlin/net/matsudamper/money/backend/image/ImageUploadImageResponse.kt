package net.matsudamper.money.backend.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageUploadImageResponse(
    @SerialName("success") val success: Success? = null,
    @SerialName("error") val error: Map<String, String> = mapOf(),
) {
    @Serializable
    data class Success(
        val hash: String,
        val url: String,
    )
}
