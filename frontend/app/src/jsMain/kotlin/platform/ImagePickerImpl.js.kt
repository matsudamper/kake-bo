package platform

import kotlin.coroutines.resume
import kotlin.js.Promise
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import net.matsudamper.money.frontend.common.base.platform.ImagePicker
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.HTMLInputElement

internal class ImagePickerImpl : ImagePicker {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun pickImages(): List<ImagePicker.SelectedImage> = suspendCancellableCoroutine { continuation ->
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = "image/*"
        input.multiple = true
        input.onchange = { _ ->
            val files = input.files
            if (files == null || files.length == 0) {
                if (continuation.isActive) {
                    continuation.resume(emptyList())
                }
            } else {
                val promises = (0 until files.length).mapNotNull { index ->
                    files.item(index)?.let { file ->
                        file.asDynamic().arrayBuffer()
                            .unsafeCast<Promise<ArrayBuffer>>()
                            .then { buffer ->
                                val bytes = toByteArray(buffer)
                                if (bytes.isNotEmpty()) {
                                    ImagePicker.SelectedImage(
                                        id = Uuid.random().toString(),
                                        previewBytes = bytes,
                                        await = {
                                            ImagePicker.UploadedImageData(
                                                bytes = bytes,
                                                contentType = file.type.ifBlank { "application/octet-stream" },
                                            )
                                        },
                                    )
                                } else {
                                    null
                                }
                            }
                    }
                }
                Promise.all(promises.toTypedArray()).then { selectedImages ->
                    if (continuation.isActive) {
                        continuation.resume(
                            selectedImages
                                .unsafeCast<Array<ImagePicker.SelectedImage?>>()
                                .filterNotNull(),
                        )
                    }
                }.catch {
                    if (continuation.isActive) {
                        continuation.resume(emptyList())
                    }
                }
            }
            Unit
        }
        input.click()
    }

    private fun toByteArray(buffer: ArrayBuffer): ByteArray {
        val int8Array = Int8Array(buffer)
        return ByteArray(int8Array.length) { index ->
            (int8Array.asDynamic()[index] as Number).toInt().toByte()
        }
    }
}
