package net.matsudamper.money.platform

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import net.matsudamper.money.frontend.common.base.image.SelectedImage
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
            val imageBytes = withContext(Dispatchers.IO) {
                runCatching {
                    componentActivity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
            }
            SelectedImage(
                id = uri.toString(),
                bytes = imageBytes,
                contentType = componentActivity.contentResolver.getType(uri),
            )
        }
    }
}
