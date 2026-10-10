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
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.image.SelectedImage
import net.matsudamper.money.ui.root.platform.ImagePicker

private const val TAG = "ImagePickerImpl"

internal class ImagePickerImpl(
    private val componentActivity: ComponentActivity,
) : ImagePicker {
    private val multipleChannel = MutableSharedFlow<List<Uri>>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val multipleLauncher = componentActivity.registerForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
    ) { uris ->
        multipleChannel.tryEmit(uris)
    }
    private val singleChannel = MutableSharedFlow<Uri?>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val singleLauncher = componentActivity.registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        singleChannel.tryEmit(uri)
    }

    override suspend fun pickImages(): List<SelectedImage> {
        multipleChannel.tryEmit(listOf())
        val uris = multipleChannel
            .onStart { multipleLauncher.launch("image/*") }
            .first()

        return uris.map { uri -> toSelectedImage(uri) }
    }

    override suspend fun pickImage(): SelectedImage? {
        singleChannel.tryEmit(null)
        val uri = singleChannel
            .onStart { singleLauncher.launch("image/*") }
            .first()
            ?: return null

        return toSelectedImage(uri)
    }

    private suspend fun toSelectedImage(uri: Uri): SelectedImage {
        val imageBytes = withContext(Dispatchers.IO) {
            runCatching {
                componentActivity.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.onFailure {
                Logger.e(TAG, it)
            }.getOrNull()
        }
        return SelectedImage(
            id = uri.toString(),
            bytes = imageBytes,
            contentType = componentActivity.contentResolver.getType(uri),
        )
    }
}
