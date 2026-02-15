package net.matsudamper.money.frontend.common.base

import net.matsudamper.money.element.ImageId

public interface ImageUploadClient {
    public suspend fun upload(
        bytes: ByteArray,
        contentType: String?,
    ): ImageId?
}
