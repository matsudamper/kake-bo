package net.matsudamper.money.frontend.common.di

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.frontend.common.base.ImageUploadClient
import net.matsudamper.money.image.ImageApiPath
import net.matsudamper.money.image.ImageUploadImageResponse
import org.khronos.webgl.Int8Array
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.xhr.FormData

public class ImageUploadClientJsImpl : ImageUploadClient {
    override suspend fun upload(
        bytes: ByteArray,
        contentType: String?,
    ): ImageId? {
        if (bytes.isEmpty()) return null

        val int8Array = Int8Array(bytes.size)
        bytes.forEachIndexed { index, byte ->
            int8Array.asDynamic()[index] = byte.toInt()
        }
        val blob = Blob(
            arrayOf(int8Array.buffer),
            BlobPropertyBag(type = contentType.orEmpty().ifBlank { "application/octet-stream" }),
        )
        val formData = FormData()
        formData.append("file", blob, "image")

        val init = js("({})")
        init.method = "POST"
        init.body = formData
        init.credentials = "include"

        return runCatching {
            val response = window.fetch(ImageApiPath.uploadV1, init).await()
            val body = response.text().await()
            Json.decodeFromString<ImageUploadImageResponse>(body).success?.imageId
        }.getOrNull()
    }
}
