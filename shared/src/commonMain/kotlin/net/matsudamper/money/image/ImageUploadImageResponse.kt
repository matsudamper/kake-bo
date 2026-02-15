package net.matsudamper.money.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.matsudamper.money.element.ImageId

@Serializable
public data class ImageUploadImageResponse(
    @SerialName("success") val success: Success? = null,
    @SerialName("error") val error: Map<String, String> = mapOf(),
) {
    @Serializable
    public data class Success(
        val imageId: ImageId,
        val url: String,
    )
}
