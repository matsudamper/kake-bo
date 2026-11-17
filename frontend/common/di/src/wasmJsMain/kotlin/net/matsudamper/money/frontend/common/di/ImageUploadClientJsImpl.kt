package net.matsudamper.money.frontend.common.di

import kotlin.js.toJsArray
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import net.matsudamper.money.frontend.common.base.ImageUploadClient
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.image.ImageUploadApiPath
import net.matsudamper.money.image.ImageUploadImageResponse
import org.khronos.webgl.toInt8Array
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.xhr.FormData

private const val TAG = "ImageUploadClientJsImpl"

public class ImageUploadClientJsImpl : ImageUploadClient {
    override suspend fun upload(
        bytes: ByteArray,
        contentType: String?,
    ): ImageUploadClient.UploadResult? {
        if (bytes.isEmpty()) return null

        val blob = Blob(
            arrayOf<JsAny?>(bytes.toInt8Array().buffer).toJsArray(),
            BlobPropertyBag(type = contentType.orEmpty().ifBlank { "application/octet-stream" }),
        )
        val formData = FormData()
        formData.append("file", blob, "image")

        val init = createUploadRequestInit(body = formData)

        return runCatching {
            val response = window.fetch(ImageUploadApiPath.uploadV1, init).await<Response>()
            val body = response.text().await<JsString>().toString()
            val success = Json.decodeFromString<ImageUploadImageResponse>(body).success ?: return@runCatching null
            ImageUploadClient.UploadResult(
                imageId = success.imageId,
                url = success.url,
                thumbnailUrl = success.thumbnailUrl,
            )
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull()
    }
}

// RequestInit() は未指定の項目を null で埋め、ブラウザが cache: null などを不正な値として fetch を拒否する
private fun createUploadRequestInit(body: FormData): RequestInit = js("({ method: 'POST', body: body, credentials: 'include' })")
