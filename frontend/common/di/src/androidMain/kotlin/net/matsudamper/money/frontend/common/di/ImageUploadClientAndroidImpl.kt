package net.matsudamper.money.frontend.common.di

import androidx.datastore.core.DataStore
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.matsudamper.money.frontend.common.base.ImageUploadClient
import net.matsudamper.money.frontend.common.feature.localstore.generated.Session
import net.matsudamper.money.frontend.graphql.serverHost
import net.matsudamper.money.frontend.graphql.serverProtocol
import net.matsudamper.money.image.ImageUploadApiPath
import net.matsudamper.money.image.ImageUploadImageResponse

private const val UserSessionIdKey = "user_session_id"

public class ImageUploadClientAndroidImpl(
    private val sessionDataStore: DataStore<Session>,
) : ImageUploadClient {
    override suspend fun upload(
        bytes: ByteArray,
        contentType: String?,
    ): ImageUploadClient.UploadResult? {
        return withContext(Dispatchers.IO) {
            if (bytes.isEmpty()) return@withContext null

            val session = sessionDataStore.data.firstOrNull()
            val host = session?.serverHost.orEmpty().ifEmpty { serverHost }
            if (host.isBlank()) return@withContext null

            val protocol = serverProtocol.ifBlank { "https" }
            val requestUrl = "$protocol://$host${ImageUploadApiPath.uploadV1}"
            val boundary = "----kakebo-${UUID.randomUUID()}"
            val resolvedContentType = contentType.orEmpty().ifBlank { "application/octet-stream" }
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
                    output.write("Content-Type: $resolvedContentType\r\n\r\n".toByteArray())
                    output.write(bytes)
                    output.write("\r\n--$boundary--\r\n".toByteArray())
                    output.flush()
                }

                val responseBody = (
                    if (connection.responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }
                    )?.let { stream ->
                    BufferedReader(InputStreamReader(stream)).use { it.readText() }
                }.orEmpty()
                connection.disconnect()

                val success = Json.decodeFromString<ImageUploadImageResponse>(responseBody).success
                    ?: return@runCatching null
                ImageUploadClient.UploadResult(
                    imageId = success.imageId,
                    url = success.url,
                )
            }.getOrNull()
        }
    }
}
