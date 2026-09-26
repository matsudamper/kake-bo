package net.matsudamper.money.frontend.common.feature.uploader

internal class ImageUploadLocalStorageJsImpl : ImageUploadLocalStorage {
    private val rawImages = mutableMapOf<String, ByteArray>()
    private val previews = mutableMapOf<String, ByteArray>()

    override suspend fun writeRawImage(id: String, bytes: ByteArray) {
        rawImages[id] = bytes
    }

    override suspend fun writePreview(id: String, bytes: ByteArray) {
        previews[id] = bytes
    }

    override suspend fun readRawImage(id: String): ByteArray? {
        return rawImages[id]
    }

    override suspend fun readPreview(id: String): ByteArray? {
        return previews[id]
    }

    override suspend fun deleteImages(id: String) {
        rawImages.remove(id)
        previews.remove(id)
    }
}
