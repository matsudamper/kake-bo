package net.matsudamper.money.frontend.common.base

import net.matsudamper.money.element.ImageId

public interface ImageUploadClient {
    public data class UploadResult(
        val imageId: ImageId,
        val url: String,
    )

    public suspend fun upload(
        bytes: ByteArray,
        contentType: String?,
    ): UploadResult?
}
