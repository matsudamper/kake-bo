package net.matsudamper.money.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import net.matsudamper.money.frontend.common.ui.layout.image.SelectedImage
import net.matsudamper.money.ui.root.platform.ImagePicker

internal class ImagePickerImpl(
    private val componentActivity: ComponentActivity,
) : ImagePicker {
    private val channel = MutableSharedFlow<Uri?>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val launcher = componentActivity.registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        channel.tryEmit(uri)
    }

    override suspend fun pickImage(): SelectedImage? {
        channel.tryEmit(null)
        val uri = channel
            .onStart { launcher.launch("image/*") }
            .first()
            ?: return null

        val bytes = try {
            withContext(Dispatchers.IO) {
                componentActivity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        }
        if (bytes == null || bytes.isEmpty()) {
            return null
        }
        val webpBytes = convertToWebp(bytes)
        if (webpBytes != null) {
            return SelectedImage(
                bytes = webpBytes,
                contentType = "image/webp",
            )
        }
        return null
    }

    private suspend fun convertToWebp(bytes: ByteArray): ByteArray? {
        return withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
            try {
                val output = ByteArrayOutputStream()
                if (!bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, WEBP_QUALITY, output)) {
                    null
                } else {
                    output.toByteArray()
                }
            } finally {
                bitmap.recycle()
            }
        }
    }

    companion object {
        private const val WEBP_QUALITY = 100
    }
}
