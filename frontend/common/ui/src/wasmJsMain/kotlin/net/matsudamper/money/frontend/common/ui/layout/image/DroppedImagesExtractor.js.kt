package net.matsudamper.money.frontend.common.ui.layout.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.domDataTransferOrNull
import kotlin.js.Promise
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.await
import net.matsudamper.money.frontend.common.base.image.DroppedImages
import net.matsudamper.money.frontend.common.base.image.SelectedImage
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.w3c.dom.DataTransfer
import org.w3c.files.File

private const val FILES_TRANSFER_TYPE = "Files"

@Composable
internal actual fun rememberDroppedImagesExtractor(): DroppedImagesExtractor {
    return remember { JsDroppedImagesExtractor() }
}

@OptIn(ExperimentalComposeUiApi::class)
private class JsDroppedImagesExtractor : DroppedImagesExtractor {
    override fun mayContainImages(event: DragAndDropEvent): Boolean {
        val dataTransfer = event.transferData?.domDataTransferOrNull ?: return false
        val types = dataTransfer.types
        return (0 until types.length).any { index -> types[index].toString() == FILES_TRANSFER_TYPE }
    }

    override fun extract(event: DragAndDropEvent): DroppedImages? {
        val dataTransfer = event.transferData?.domDataTransferOrNull ?: return null
        // DataTransfer の中身はドロップイベントを抜けると読めなくなるので、File だけ先に取り出しておく
        val imageFiles = dataTransfer.imageFiles()
        if (imageFiles.isEmpty()) return null
        return object : DroppedImages {
            override suspend fun read(): List<SelectedImage> {
                return imageFiles.mapNotNull { file ->
                    runCatching { file.toSelectedImage() }.getOrNull()
                }
            }
        }
    }

    private fun DataTransfer.imageFiles(): List<File> {
        val fileList = files
        return (0 until fileList.length)
            .mapNotNull { index -> fileList.item(index) }
            .filter { file -> file.type.startsWith("image/") }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun File.toSelectedImage(): SelectedImage? {
        val bytes = Int8Array(readArrayBuffer(this).await<ArrayBuffer>()).toByteArray()
        if (bytes.isEmpty()) return null
        return SelectedImage(
            id = Uuid.random().toString(),
            bytes = bytes,
            contentType = type,
        )
    }
}

private fun readArrayBuffer(file: File): Promise<ArrayBuffer> = js("file.arrayBuffer()")
