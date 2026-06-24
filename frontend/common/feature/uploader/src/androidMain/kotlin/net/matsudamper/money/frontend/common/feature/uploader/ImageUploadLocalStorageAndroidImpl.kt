package net.matsudamper.money.frontend.common.feature.uploader

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ImageUploadLocalStorageAndroidImpl(
    private val context: Context,
) : ImageUploadLocalStorage {

    private fun rawImageFile(id: String): File =
        File(context.cacheDir, "image_uploads/$id.raw")

    private fun previewFile(id: String): File =
        File(context.cacheDir, "image_uploads/$id.preview")

    override suspend fun writeRawImage(id: String, bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            rawImageFile(id).also { it.parentFile?.mkdirs() }.writeBytes(bytes)
        }
    }

    override suspend fun writePreview(id: String, bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            previewFile(id).also { it.parentFile?.mkdirs() }.writeBytes(bytes)
        }
    }

    override suspend fun readRawImage(id: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            rawImageFile(id).takeIf { it.exists() }?.readBytes()
        }
    }

    override suspend fun readPreview(id: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            // 新しいレコードはpreviewファイルを作成しないため、rawImageファイルにフォールバックする
            previewFile(id).takeIf { it.exists() }?.readBytes()
                ?: rawImageFile(id).takeIf { it.exists() }?.readBytes()
        }
    }

    override suspend fun deleteImages(id: String) {
        withContext(Dispatchers.IO) {
            rawImageFile(id).delete()
            previewFile(id).delete()
        }
    }
}
