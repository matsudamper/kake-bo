package net.matsudamper.money.frontend.common.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.MoneyUsageId
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

internal class ImageUploadWorker(
    context: Context,
    params: WorkerParameters,
    private val sessionDataStore: DataStore<Session>,
) : CoroutineWorker(context, params) {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        val filePath = inputData.getString(KEY_FILE_PATH) ?: return Result.failure()
        val contentType = inputData.getString(KEY_CONTENT_TYPE) ?: "application/octet-stream"
        val rawMoneyUsageId = inputData.getInt(KEY_MONEY_USAGE_ID, -1).takeIf { it >= 0 }
            ?: return Result.failure()
        val moneyUsageId = MoneyUsageId(rawMoneyUsageId)

        val file = File(filePath)
        if (!file.exists()) return Result.failure()

        val session = sessionDataStore.data.firstOrNull()
        val host = session?.serverHost.orEmpty().ifEmpty { serverHost }
        if (host.isBlank()) return Result.failure()
        val protocol = serverProtocol.ifBlank { "https" }
        val userSessionId = session?.userSessionId.orEmpty()

        val bytes = withContext(Dispatchers.IO) { file.readBytes() }

        val imageId = uploadImage(bytes, contentType, protocol, host, userSessionId)
            ?: return Result.retry()

        val currentImageIds = fetchCurrentImageIds(moneyUsageId, protocol, host, userSessionId)
            ?: return Result.retry()

        val updatedImageIds = (currentImageIds + imageId).distinctBy { it.value }
        val success = updateMoneyUsage(moneyUsageId, updatedImageIds, protocol, host, userSessionId)

        if (success) {
            withContext(Dispatchers.IO) { file.delete() }
        }

        return if (success) {
            Result.success(workDataOf(KEY_RESULT_IMAGE_ID to imageId.value))
        } else {
            Result.retry()
        }
    }

    private suspend fun uploadImage(
        bytes: ByteArray,
        contentType: String,
        protocol: String,
        host: String,
        sessionId: String,
    ): ImageId? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        name = "file",
                        filename = "image",
                        body = bytes.toRequestBody(contentType.toMediaTypeOrNull()),
                    )
                    .build()

                val request = Request.Builder()
                    .url("$protocol://$host${ImageUploadApiPath.uploadV1}")
                    .post(requestBody)
                    .apply {
                        if (sessionId.isNotBlank()) {
                            header("Cookie", "user_session_id=$sessionId")
                        }
                    }
                    .build()

                val responseBody = okHttpClient.newCall(request).execute().use { it.body.string() }
                Json.decodeFromString<ImageUploadImageResponse>(responseBody).success?.imageId
            }.getOrNull()
        }
    }

    private suspend fun fetchCurrentImageIds(
        moneyUsageId: MoneyUsageId,
        protocol: String,
        host: String,
        sessionId: String,
    ): List<ImageId>? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val query = """
                    query ImageUploadWorkerFetchImages(${"$"}id: MoneyUsageId!) {
                        user {
                            moneyUsage(id: ${"$"}id) {
                                images {
                                    id
                                }
                            }
                        }
                    }
                """.trimIndent()

                val variables = buildJsonObject {
                    put("id", moneyUsageId.id)
                }

                val requestBodyJson = buildJsonObject {
                    put("query", query)
                    put("variables", variables)
                }.toString()

                val request = Request.Builder()
                    .url("$protocol://$host/query")
                    .post(requestBodyJson.toRequestBody("application/json".toMediaTypeOrNull()))
                    .apply {
                        if (sessionId.isNotBlank()) {
                            header("Cookie", "user_session_id=$sessionId")
                        }
                    }
                    .build()

                val responseBody = okHttpClient.newCall(request).execute().use { it.body.string() }
                val json = Json.parseToJsonElement(responseBody).jsonObject
                json["data"]?.jsonObject
                    ?.get("user")?.jsonObject
                    ?.get("moneyUsage")?.jsonObject
                    ?.get("images")?.jsonArray
                    ?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.int?.let { id -> ImageId(id) } }
                    ?: listOf()
            }.getOrNull()
        }
    }

    private suspend fun updateMoneyUsage(
        moneyUsageId: MoneyUsageId,
        imageIds: List<ImageId>,
        protocol: String,
        host: String,
        sessionId: String,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            runCatching {
                val query = """
                    mutation MoneyUsageScreenUpdateUsage(${"$"}query: UpdateUsageQuery!) {
                        userMutation {
                            updateUsage(query: ${"$"}query) {
                                id
                            }
                        }
                    }
                """.trimIndent()

                val variables = buildJsonObject {
                    putJsonObject("query") {
                        put("id", moneyUsageId.id)
                        putJsonArray("imageIds") {
                            imageIds.forEach { add(JsonPrimitive(it.value)) }
                        }
                    }
                }

                val requestBodyJson = buildJsonObject {
                    put("query", query)
                    put("variables", variables)
                }.toString()

                val request = Request.Builder()
                    .url("$protocol://$host/query")
                    .post(requestBodyJson.toRequestBody("application/json".toMediaTypeOrNull()))
                    .apply {
                        if (sessionId.isNotBlank()) {
                            header("Cookie", "user_session_id=$sessionId")
                        }
                    }
                    .build()

                val responseBody = okHttpClient.newCall(request).execute().use { it.body.string() }
                val json = Json.parseToJsonElement(responseBody).jsonObject
                json["data"]?.jsonObject?.get("userMutation")?.jsonObject
                    ?.get("updateUsage")?.jsonObject?.get("id") != null
            }.getOrElse { false }
        }
    }

    internal companion object {
        const val KEY_FILE_PATH = "file_path"
        const val KEY_CONTENT_TYPE = "content_type"
        const val KEY_MONEY_USAGE_ID = "money_usage_id"
        const val KEY_RESULT_IMAGE_ID = "result_image_id"
    }
}
