package platform

import kotlin.coroutines.resume
import kotlin.js.Promise
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import net.matsudamper.money.frontend.common.base.image.SelectedImage
import net.matsudamper.money.ui.root.platform.ImagePicker
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.files.File

internal class ImagePickerImpl : ImagePicker {
    override suspend fun pickImages(): List<SelectedImage> {
        val files = pickFiles()
        return runCatching {
            files.mapNotNull { file -> file.toSelectedImage() }
        }.getOrElse { emptyList() }
    }

    private suspend fun pickFiles(): List<File> = suspendCancellableCoroutine { continuation ->
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = "image/*"
        input.multiple = true
        input.style.display = "none"
        document.body?.appendChild(input)

        var resolved = false
        var focusTimeoutId: Int? = null
        var pickerEngaged = false
        lateinit var cancelHandler: (Event) -> Unit
        lateinit var focusHandler: (Event) -> Unit

        fun cleanup() {
            focusTimeoutId?.let { window.clearTimeout(it) }
            window.removeEventListener("focus", focusHandler)
            input.removeEventListener("cancel", cancelHandler)
            input.parentNode?.removeChild(input)
        }

        fun resumeOnce(files: List<File>) {
            if (!continuation.isActive || resolved) {
                return
            }
            resolved = true
            cleanup()
            continuation.resume(files)
        }

        cancelHandler = {
            resumeOnce(emptyList())
        }

        focusHandler = onWindowFocus@{
            if (!pickerEngaged) {
                return@onWindowFocus
            }
            focusTimeoutId?.let { window.clearTimeout(it) }
            focusTimeoutId = window.setTimeout({
                val files = input.files
                if (files == null || files.length == 0) {
                    resumeOnce(emptyList())
                }
                null
            }, FOCUS_SETTLE_DELAY_MS)
        }

        continuation.invokeOnCancellation {
            if (!resolved) {
                resolved = true
                cleanup()
            }
        }

        input.addEventListener("cancel", cancelHandler)
        window.addEventListener("focus", focusHandler)

        input.onchange = { _ ->
            val files = input.files
            resumeOnce(
                if (files == null) {
                    emptyList()
                } else {
                    (0 until files.length).mapNotNull { index -> files.item(index) }
                },
            )
        }

        pickerEngaged = true
        input.click()
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun File.toSelectedImage(): SelectedImage? {
        val bytes = Int8Array(readArrayBuffer(this).await<ArrayBuffer>()).toByteArray()
        if (bytes.isEmpty()) return null
        return SelectedImage(
            id = Uuid.random().toString(),
            bytes = bytes,
            contentType = type.ifBlank { "application/octet-stream" },
        )
    }

    private companion object {
        private const val FOCUS_SETTLE_DELAY_MS = 300
    }
}

private fun readArrayBuffer(file: File): Promise<ArrayBuffer> = js("file.arrayBuffer()")
