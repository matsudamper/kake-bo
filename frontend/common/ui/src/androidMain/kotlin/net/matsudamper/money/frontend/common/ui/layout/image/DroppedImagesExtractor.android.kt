package net.matsudamper.money.frontend.common.ui.layout.image

import android.app.Activity
import android.content.ClipData
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.image.DroppedImages
import net.matsudamper.money.frontend.common.base.image.ImageCapturedDateTimeReader
import net.matsudamper.money.frontend.common.base.image.SelectedImage

private const val TAG = "DroppedImagesExtractor"
private const val IMAGE_MIME_TYPE = "image/*"

@Composable
internal actual fun rememberDroppedImagesExtractor(): DroppedImagesExtractor {
    val activity = LocalActivity.current
    return remember(activity) { AndroidDroppedImagesExtractor(activity) }
}

private class AndroidDroppedImagesExtractor(
    private val activity: Activity?,
) : DroppedImagesExtractor {
    override fun mayContainImages(event: DragAndDropEvent): Boolean {
        return event.toAndroidDragEvent().clipDescription?.hasMimeType(IMAGE_MIME_TYPE) == true
    }

    override fun extract(event: DragAndDropEvent): DroppedImages? {
        val activity = activity ?: return null
        val dragEvent = event.toAndroidDragEvent()
        val clipData = dragEvent.clipData ?: return null
        if (!clipData.description.hasMimeType(IMAGE_MIME_TYPE)) return null
        val uris = clipData.uris()
        if (uris.isEmpty()) return null

        // 他アプリからの content:// を読むには、ドロップイベント中に権限を取得しておく必要がある
        val permissions = activity.requestDragAndDropPermissions(dragEvent)
        return object : DroppedImages {
            override suspend fun read(): List<SelectedImage> {
                return try {
                    // 権限を手放した後に URI を読み直す経路へ回さないよう、読めなかった画像はここで落とす
                    uris.mapNotNull { uri -> activity.readSelectedImage(uri) }
                } finally {
                    permissions?.release()
                }
            }
        }
    }

    private fun ClipData.uris(): List<Uri> {
        return (0 until itemCount).mapNotNull { index -> getItemAt(index).uri }
    }

    private suspend fun Activity.readSelectedImage(uri: Uri): SelectedImage? {
        val imageBytes = withContext(Dispatchers.IO) {
            runCatching {
                contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.onFailure {
                Logger.e(TAG, it)
            }.getOrNull()
        } ?: return null
        return SelectedImage(
            id = uri.toString(),
            bytes = imageBytes,
            contentType = contentResolver.getType(uri),
            capturedDateTime = ImageCapturedDateTimeReader.read(imageBytes),
        )
    }
}
