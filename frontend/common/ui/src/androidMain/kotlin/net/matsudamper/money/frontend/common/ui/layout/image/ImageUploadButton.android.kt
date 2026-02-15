package net.matsudamper.money.frontend.common.ui.layout.image

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.frontend.common.feature.localstore.DataStores
import net.matsudamper.money.frontend.graphql.serverHost
import net.matsudamper.money.frontend.graphql.serverProtocol
import net.matsudamper.money.image.ImageApiPath
import net.matsudamper.money.image.ImageUploadImageResponse

private const val UserSessionIdKey = "user_session_id"

@Composable
public actual fun ImageUploadButton(
    onUploaded: (ImageId) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val imageId = uploadImage(
                uri = uri,
                context = context,
            ) ?: return@launch
            onUploaded(imageId)
        }
    }

    Button(
        modifier = modifier,
        onClick = {
            launcher.launch("image/*")
        },
    ) {
        Text("画像をアップロード")
    }
}

private suspend fun uploadImage(
    uri: Uri,
    context: android.content.Context,
): ImageId? {
    return withContext(Dispatchers.IO) {
        val session = DataStores.create(context).sessionDataStore.data.firstOrNull()
        val host = session?.serverHost.orEmpty().ifEmpty { serverHost }
        if (host.isBlank()) return@withContext null

        val fileBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@withContext null
        if (fileBytes.isEmpty()) return@withContext null

        val protocol = serverProtocol.ifBlank { "https" }
        val requestUrl = "$protocol://$host${ImageApiPath.uploadV1}"
        val boundary = "----kakebo-${UUID.randomUUID()}"
        val contentType = context.contentResolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" }
        val userSessionId = session?.userSessionId.orEmpty()

        runCatching {
            val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 5_000
                readTimeout = 10_000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                if (userSessionId.isNotBlank()) {
                    setRequestProperty("Cookie", "$UserSessionIdKey=$userSessionId")
                }
            }

            connection.outputStream.buffered().use { output ->
                output.write("--$boundary\r\n".toByteArray())
                output.write("Content-Disposition: form-data; name=\"file\"; filename=\"image\"\r\n".toByteArray())
                output.write("Content-Type: $contentType\r\n\r\n".toByteArray())
                output.write(fileBytes)
                output.write("\r\n--$boundary--\r\n".toByteArray())
                output.flush()
            }

            val responseBody = (if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.let { stream ->
                BufferedReader(InputStreamReader(stream)).use { it.readText() }
            }.orEmpty()

            connection.disconnect()
            Json.decodeFromString<ImageUploadImageResponse>(responseBody).success?.imageId
        }.getOrNull()
    }
}
