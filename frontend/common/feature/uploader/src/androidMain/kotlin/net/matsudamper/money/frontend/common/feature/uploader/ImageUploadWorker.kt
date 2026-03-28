package net.matsudamper.money.frontend.common.feature.uploader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.feature.localstore.DataStores
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.MoneyUsageScreenQuery
import net.matsudamper.money.frontend.graphql.MoneyUsageScreenUpdateUsageMutation
import net.matsudamper.money.frontend.graphql.ServerHostConfig
import net.matsudamper.money.frontend.graphql.serverHost
import net.matsudamper.money.frontend.graphql.serverProtocol
import net.matsudamper.money.frontend.graphql.type.UpdateUsageQuery
import net.matsudamper.money.image.ImageUploadApiPath
import net.matsudamper.money.image.ImageUploadImageResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class ImageUploadWorker(
    appContext: Context,
    params: WorkerParameters,
    private val dao: ImageUploadDao,
    private val dataStores: DataStores,
    private val graphqlClient: GraphqlClient,
    private val serverHostConfig: ServerHostConfig,
) : CoroutineWorker(appContext, params) {

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun doWork(): Result {
        val recordId = inputData.getString(KEY_RECORD_ID) ?: return Result.failure()

        dao.updateWorkManagerId(recordId, id.toString())
        dao.updateStatus(recordId, ImageUploadQueueImpl.STATUS_UPLOADING)

        try {
            setForeground(createForegroundInfo())
        } catch (_: Exception) {
            // 通知権限がない場合はForegroundServiceなしで継続
        }

        val entity = dao.getById(recordId) ?: return Result.failure()
        val moneyUsageId = entity.moneyUsageId

        return try {
            doUploadWork(recordId)
        } finally {
            withContext(NonCancellable) {
                triggerNextPending(moneyUsageId, recordId)
            }
        }
    }

    private suspend fun doUploadWork(recordId: String): Result {
        val rawImageBytes = withContext(Dispatchers.IO) {
            runCatching { rawImageBytesFile(applicationContext, recordId).readBytes() }.getOrNull()
        }
        if (rawImageBytes == null) {
            // キャッシュがクリアされてファイルが消えた場合はDBレコードも削除してクリーンアップ
            dao.deleteById(recordId)
            return Result.success()
        }

        val webpBytes = withContext(Dispatchers.Default) {
            convertToWebP(rawImageBytes)
        }
        if (webpBytes == null) {
            dao.updateStatusWithError(recordId, ImageUploadQueueImpl.STATUS_FAILED, "画像変換に失敗しました")
            return Result.failure()
        }

        val session = dataStores.sessionDataStore.data.firstOrNull()
        val userSessionId = session?.userSessionId.orEmpty()
        val savedHost = session?.serverHost.orEmpty()
        val host = savedHost.ifEmpty {
            serverHostConfig.savedHost.ifEmpty {
                serverHostConfig.defaultHost.ifEmpty { serverHost }
            }
        }
        val protocol = serverHostConfig.protocol.ifEmpty { serverProtocol.ifEmpty { "https" } }

        val uploadedImageId = withContext(Dispatchers.IO) {
            uploadImage(
                bytes = webpBytes,
                host = host,
                protocol = protocol,
                userSessionId = userSessionId,
            )
        }
        if (uploadedImageId == null) {
            dao.updateStatusWithError(recordId, ImageUploadQueueImpl.STATUS_FAILED, "アップロードに失敗しました")
            return Result.failure()
        }

        val entity = dao.getById(recordId) ?: return Result.failure()
        val moneyUsageId = MoneyUsageId(id = entity.moneyUsageId)
        val currentImageIds = runCatching {
            graphqlClient.apolloClient
                .query(MoneyUsageScreenQuery(id = moneyUsageId))
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
                .data?.user?.moneyUsage?.moneyUsageScreenMoneyUsage?.images
                ?.map { it.id }
        }.getOrNull()

        val updatedImageIds = ((currentImageIds ?: listOf()) + uploadedImageId)
            .distinctBy { it.value }

        val isSuccess = runCatching {
            graphqlClient.apolloClient
                .mutation(
                    MoneyUsageScreenUpdateUsageMutation(
                        query = UpdateUsageQuery(
                            id = moneyUsageId,
                            imageIds = Optional.present(updatedImageIds),
                        ),
                    ),
                )
                .execute()
                .data?.userMutation?.updateUsage != null
        }.getOrDefault(false)

        if (!isSuccess) {
            dao.updateStatusWithError(recordId, ImageUploadQueueImpl.STATUS_FAILED, "使用用途の更新に失敗しました")
            return Result.failure()
        }

        dao.deleteById(recordId)
        rawImageBytesFile(applicationContext, recordId).delete()
        previewBytesFile(applicationContext, recordId).delete()
        return Result.success()
    }

    private suspend fun triggerNextPending(moneyUsageId: Int, completedRecordId: String) {
        val nextItem = dao.getOldestPendingByMoneyUsageId(moneyUsageId) ?: return
        if (nextItem.id == completedRecordId) return
        val request = OneTimeWorkRequestBuilder<ImageUploadWorker>()
            .setInputData(workDataOf(KEY_RECORD_ID to nextItem.id))
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            ImageUploadQueueImpl.uniqueWorkName(MoneyUsageId(moneyUsageId)),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun convertToWebP(bytes: ByteArray): ByteArray? {
        return runCatching {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val rotatedBitmap = rotateByExif(bytes, bitmap)
            val stream = ByteArrayOutputStream()
            rotatedBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, stream)
            if (rotatedBitmap !== bitmap) rotatedBitmap.recycle()
            bitmap.recycle()
            stream.toByteArray()
        }.getOrNull()
    }

    private fun rotateByExif(bytes: ByteArray, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            ExifInterface(bytes.inputStream()).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun uploadImage(
        bytes: ByteArray,
        host: String,
        protocol: String,
        userSessionId: String,
    ): ImageId? {
        if (host.isBlank()) return null
        val requestUrl = "$protocol://$host${ImageUploadApiPath.uploadV1}"
        return runCatching {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    name = "file",
                    filename = "image",
                    body = bytes.toRequestBody("image/webp".toMediaTypeOrNull()),
                )
                .build()

            val request = Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .apply {
                    if (userSessionId.isNotBlank()) {
                        header("Cookie", "user_session_id=$userSessionId")
                    }
                }
                .build()

            val responseBody = okHttpClient.newCall(request).execute().use { response ->
                response.body.string()
            }
            Json.decodeFromString<ImageUploadImageResponse>(responseBody).success?.imageId
        }.getOrNull()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val channelId = "image_upload"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId,
            "画像アップロード",
            NotificationManager.IMPORTANCE_LOW,
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("画像をアップロード中")
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .build()

        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    internal companion object {
        const val KEY_RECORD_ID = "record_id"
        private const val NOTIFICATION_ID = 1001
    }
}
