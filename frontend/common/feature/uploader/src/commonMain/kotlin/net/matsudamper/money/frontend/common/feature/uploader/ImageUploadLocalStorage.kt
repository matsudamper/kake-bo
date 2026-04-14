package net.matsudamper.money.frontend.common.feature.uploader

public interface ImageUploadLocalStorage {
    public suspend fun writeRawImage(id: String, bytes: ByteArray)

    public suspend fun writePreview(id: String, bytes: ByteArray)

    public suspend fun readRawImage(id: String): ByteArray?

    public suspend fun readPreview(id: String): ByteArray?

    public suspend fun deleteImages(id: String)
}
