package net.matsudamper.money.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import net.matsudamper.money.frontend.common.ui.layout.image.SelectedImage
import net.matsudamper.money.ui.root.platform.ImagePicker

internal class ImagePickerImpl(
    private val componentActivity: ComponentActivity,
) : ImagePicker {
    private var continuation: CancellableContinuation<SelectedImage?>? = null
    private val launcher = componentActivity.registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        val current = continuation ?: return@registerForActivityResult
        continuation = null

        if (uri == null) {
            current.resume(null)
            return@registerForActivityResult
        }

        val bytes = try {
            componentActivity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Throwable) {
            e.printStackTrace()
            current.resume(null)
            return@registerForActivityResult
        }
        if (bytes == null || bytes.isEmpty()) {
            current.resume(null)
            return@registerForActivityResult
        }
        val webpBytes = convertToWebp(bytes)
        if (webpBytes != null) {
            current.resume(
                SelectedImage(
                    bytes = webpBytes,
                    contentType = "image/webp",
                ),
            )
        } else {
            val contentType = componentActivity.contentResolver.getType(uri)
                .orEmpty()
                .ifBlank { "application/octet-stream" }
            current.resume(
                SelectedImage(
                    bytes = bytes,
                    contentType = contentType,
                ),
            )
        }
    }

    private fun convertToWebp(bytes: ByteArray): ByteArray? {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        return try {
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, WEBP_QUALITY, output)
            output.toByteArray()
        } finally {
            bitmap.recycle()
        }
    }

    companion object {
        private const val WEBP_QUALITY = 90
    }

    override suspend fun pickImage(): SelectedImage? = suspendCancellableCoroutine { continuation ->
        this.continuation?.resume(null)
        this.continuation = continuation
        continuation.invokeOnCancellation {
            if (this.continuation == continuation) {
                this.continuation = null
            }
        }
        launcher.launch("image/*")
    }
}
