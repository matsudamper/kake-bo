package platform

import kotlin.coroutines.resume
import kotlin.js.Date
import kotlin.js.Promise
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import net.matsudamper.money.frontend.common.base.image.SelectedImage
import net.matsudamper.money.ui.root.platform.ImagePicker
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event

internal class ImagePickerImpl : ImagePicker {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun pickImages(): List<SelectedImage> = suspendCancellableCoroutine { continuation ->
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = "image/*"
        input.multiple = true
        input.style.display = "none"
        document.body?.appendChild(input)

        var resolved = false
        var focusTimeoutId: Int? = null
        var pickerEngaged = false
        var pickerClickedAtMs = 0.0
        var windowBlurredForPicker = false
        lateinit var cancelHandler: (Event) -> Unit
        lateinit var blurHandler: (Event) -> Unit
        lateinit var focusHandler: (Event) -> Unit

        fun cleanup() {
            focusTimeoutId?.let { window.clearTimeout(it) }
            window.removeEventListener("focus", focusHandler)
            window.removeEventListener("blur", blurHandler)
            input.removeEventListener("cancel", cancelHandler)
            input.parentNode?.removeChild(input)
        }

        fun resumeOnce(images: List<SelectedImage>) {
            if (!continuation.isActive || resolved) {
                return
            }
            resolved = true
            cleanup()
            continuation.resume(images)
        }

        cancelHandler = {
            resumeOnce(emptyList())
        }

        blurHandler = {
            windowBlurredForPicker = true
        }

        focusHandler = onWindowFocus@{
            if (!pickerEngaged) {
                return@onWindowFocus
            }
            val elapsedSinceClickMs = Date.now() - pickerClickedAtMs
            if (!windowBlurredForPicker && elapsedSinceClickMs < FOCUS_IGNORE_AFTER_CLICK_MS) {
                return@onWindowFocus
            }
            windowBlurredForPicker = false
            focusTimeoutId?.let { window.clearTimeout(it) }
            focusTimeoutId = window.setTimeout({
                val files = input.files
                if (files == null || files.length == 0) {
                    resumeOnce(emptyList())
                }
            }, FOCUS_SETTLE_DELAY_MS)
        }

        continuation.invokeOnCancellation {
            if (!resolved) {
                resolved = true
                cleanup()
            }
        }

        input.addEventListener("cancel", cancelHandler)
        window.addEventListener("blur", blurHandler)
        window.addEventListener("focus", focusHandler)

        input.onchange = { _ ->
            val files = input.files
            if (files == null || files.length == 0) {
                resumeOnce(emptyList())
            } else {
                val promises = (0 until files.length).mapNotNull { index ->
                    files.item(index)?.let { file ->
                        file.asDynamic().arrayBuffer()
                            .unsafeCast<Promise<ArrayBuffer>>()
                            .then { buffer ->
                                val bytes = toByteArray(buffer)
                                if (bytes.isNotEmpty()) {
                                    SelectedImage(
                                        id = Uuid.random().toString(),
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
                    resumeOnce(
                        selectedImages
                            .unsafeCast<Array<SelectedImage?>>()
                            .filterNotNull(),
                    )
                }.catch {
                    resumeOnce(emptyList())
                }
            }
            Unit
        }

        pickerEngaged = true
        pickerClickedAtMs = Date.now()
        input.click()
    }

    private fun toByteArray(buffer: ArrayBuffer): ByteArray {
        val int8Array = Int8Array(buffer)
        return ByteArray(int8Array.length) { index ->
            (int8Array.asDynamic()[index] as Number).toInt().toByte()
        }
    }

    private companion object {
        private const val FOCUS_IGNORE_AFTER_CLICK_MS = 300
        private const val FOCUS_SETTLE_DELAY_MS = 300
    }
}
