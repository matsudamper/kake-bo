package net.matsudamper.money.frontend.common.viewmodel.addmoneyusage

import net.matsudamper.money.element.ImageId

internal expect object ImageUploadClient {
    suspend fun upload(
        bytes: ByteArray,
        contentType: String?,
    ): ImageId?
}
