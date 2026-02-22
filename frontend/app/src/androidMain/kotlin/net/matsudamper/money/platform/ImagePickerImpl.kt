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
import net.matsudamper.money.frontend.common.ui.layout.image.UploadedImageData
import net.matsudamper.money.ui.root.platform.ImagePicker

internal class ImagePickerImpl(
    private val componentActivity: ComponentActivity,
) : ImagePicker {
    private val channel = MutableSharedFlow<List<Uri>>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val launcher = componentActivity.registerForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
    ) { uris ->
        channel.tryEmit(uris)
    }

    override suspend fun pickImages(): List<SelectedImage> {
        channel.tryEmit(emptyList())
        val uris = channel
            .onStart { launcher.launch("image/*") }
            .first()

        return uris.map { uri ->
            SelectedImage(
                await = {
                    val bytes = try {
                        withContext(Dispatchers.IO) {
                            componentActivity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        null
                    }
                    val imageBytes = bytes ?: return@SelectedImage null
                    if (imageBytes.isEmpty()) {
                        return@SelectedImage null
                    }
                    val webpBytes = convertToWebp(imageBytes) ?: return@SelectedImage null
                    UploadedImageData(
                        bytes = webpBytes,
                        contentType = "image/webp",
                    )
                },
            )
        }
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
