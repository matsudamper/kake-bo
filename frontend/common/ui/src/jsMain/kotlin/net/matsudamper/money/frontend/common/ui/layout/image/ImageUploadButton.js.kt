package net.matsudamper.money.frontend.common.ui.layout.image

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.image.ImageApiPath
import net.matsudamper.money.image.ImageUploadImageResponse
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import org.w3c.xhr.FormData

@Composable
public actual fun ImageUploadButton(
    onUploaded: (ImageId) -> Unit,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()

    Button(
        modifier = modifier,
        onClick = {
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.accept = "image/*"
            input.onchange = {
                val file = input.files?.item(0)
                if (file != null) {
                    scope.launch {
                        val imageId = upload(file) ?: return@launch
                        onUploaded(imageId)
                    }
                }
                null
            }
            input.click()
        },
    ) {
        Text("画像をアップロード")
    }
}

private suspend fun upload(file: File): ImageId? {
    val formData = FormData()
    formData.append("file", file)

    val init = js("({})")
    init.method = "POST"
    init.body = formData
    init.credentials = "include"

    return runCatching {
        val response = window.fetch(ImageApiPath.uploadV1, init).await()
        val body = response.text().await()
        Json.decodeFromString<ImageUploadImageResponse>(body).success?.imageId
    }.getOrNull()
}
