package net.matsudamper.money.frontend.common.di

import androidx.datastore.core.DataStore
import java.util.concurrent.TimeUnit
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val UserSessionIdKey = "user_session_id"

public class ImageUploadClientAndroidImpl(
    private val sessionDataStore: DataStore<Session>,
) : ImageUploadClient {
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

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
            val resolvedContentType = contentType.orEmpty().ifBlank { "application/octet-stream" }
            val userSessionId = session?.userSessionId.orEmpty()

            runCatching {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        name = "file",
                        filename = "image",
                        body = bytes.toRequestBody(resolvedContentType.toMediaTypeOrNull()),
                    )
                    .build()

                val request = Request.Builder()
                    .url(requestUrl)
                    .post(requestBody)
                    .apply {
                        if (userSessionId.isNotBlank()) {
                            header("Cookie", "$UserSessionIdKey=$userSessionId")
                        }
                    }
                    .build()

                val responseBody = okHttpClient.newCall(request).execute().use { response ->
                    response.body.string()
                }

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
