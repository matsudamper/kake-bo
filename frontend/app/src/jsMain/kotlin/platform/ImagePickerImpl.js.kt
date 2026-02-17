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
    override suspend fun pickImage(): SelectedImage? = suspendCancellableCoroutine { continuation ->
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = "image/*"
        input.onchange = { _ ->
            val file = input.files?.item(0)
            if (file == null) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            } else {
                val promise = file.asDynamic().arrayBuffer()
                    .unsafeCast<Promise<ArrayBuffer>>()
                promise.then { buffer ->
                    if (continuation.isActive) {
                        val bytes = toByteArray(buffer)
                        continuation.resume(
                            if (bytes.isNotEmpty()) {
                                SelectedImage(
                                    bytes = bytes,
                                    contentType = file.type.ifBlank { "application/octet-stream" },
                                )
                            } else {
                                null
                            },
                        )
                    }
                }.catch {
                    if (continuation.isActive) {
                        continuation.resume(null)
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
