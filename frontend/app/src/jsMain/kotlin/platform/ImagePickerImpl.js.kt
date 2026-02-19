package platform

import kotlin.coroutines.resume
import kotlin.js.Promise
import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import net.matsudamper.money.frontend.common.ui.layout.image.SelectedImage
import net.matsudamper.money.ui.root.platform.ImagePicker
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.HTMLInputElement

internal class ImagePickerImpl : ImagePicker {
    override suspend fun pickImages(): List<SelectedImage> = suspendCancellableCoroutine { continuation ->
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
                                    SelectedImage(
                                        bytes = bytes,
                                        contentType = file.type.ifBlank { "application/octet-stream" },
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
                                .unsafeCast<Array<SelectedImage?>>()
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
