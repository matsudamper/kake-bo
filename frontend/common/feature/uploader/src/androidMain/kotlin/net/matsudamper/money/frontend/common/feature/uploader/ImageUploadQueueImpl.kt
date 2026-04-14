package net.matsudamper.money.frontend.common.feature.uploader

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.matsudamper.money.element.MoneyUsageId

internal class ImageUploadQueueImpl(
    private val context: Context,
    private val dao: ImageUploadRoomDao,
) : ImageUploadQueue {

    override fun observeItems(moneyUsageId: MoneyUsageId): Flow<List<ImageUploadQueue.QueueItem>> {
        return dao.observeByMoneyUsageId(moneyUsageId.id).map { entities ->
            entities.map { entity ->
                val previewBytes = previewBytesFile(context, entity.id)
                    .takeIf { it.exists() }
                    ?.readBytes()
                ImageUploadQueue.QueueItem(
                    id = entity.id,
                    previewBytes = previewBytes,
                    status = when (entity.status) {
                        STATUS_UPLOADING -> ImageUploadQueue.Status.Uploading
                        STATUS_COMPLETED -> ImageUploadQueue.Status.Completed
                        STATUS_FAILED -> ImageUploadQueue.Status.Failed(entity.errorMessage)
                        else -> ImageUploadQueue.Status.Pending
                    },
                )
            }
        }
    }

    override suspend fun enqueue(
        moneyUsageId: MoneyUsageId,
        rawImageBytes: ByteArray,
        previewBytes: ByteArray?,
    ) {
        val id = UUID.randomUUID().toString()
        withContext(Dispatchers.IO) {
            rawImageBytesFile(context, id).also { it.parentFile?.mkdirs() }.writeBytes(rawImageBytes)
            if (previewBytes != null) {
                previewBytesFile(context, id).also { it.parentFile?.mkdirs() }.writeBytes(previewBytes)
            }
        }
        dao.insert(
            ImageUploadRoomEntity(
                id = id,
                moneyUsageId = moneyUsageId.id,
                status = STATUS_PENDING,
                workManagerId = null,
                errorMessage = null,
                stackTrace = null,
                createdAt = System.currentTimeMillis(),
                rawImageBytes = null,
                previewBytes = null,
            ),
        )
        val request = OneTimeWorkRequestBuilder<ImageUploadWorker>()
            .setInputData(workDataOf(ImageUploadWorker.KEY_RECORD_ID to id))
            .build()
        // KEEP: 同じmoneyUsageIdのWorkerが実行中なら無視。Workerのfinally内で次のPENDINGアイテムをトリガーする。
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(moneyUsageId),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    override suspend fun retry(itemId: String) {
        val entity = dao.getById(itemId) ?: return
        dao.updateStatusWithError(itemId, STATUS_PENDING, null, null)
        val request = OneTimeWorkRequestBuilder<ImageUploadWorker>()
            .setInputData(workDataOf(ImageUploadWorker.KEY_RECORD_ID to itemId))
            .build()
        // KEEP: 実行中のWorkerがあればそのfinally内でこのアイテムが拾われる
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(MoneyUsageId(entity.moneyUsageId)),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    override fun observeAllDebugItems(): Flow<List<ImageUploadQueue.DebugItem>> {
        return dao.observeAll().map { entities ->
            entities.map { entity ->
                ImageUploadQueue.DebugItem(
                    id = entity.id,
                    moneyUsageId = entity.moneyUsageId,
                    status = when (entity.status) {
                        STATUS_UPLOADING -> ImageUploadQueue.Status.Uploading
                        STATUS_COMPLETED -> ImageUploadQueue.Status.Completed
                        STATUS_FAILED -> ImageUploadQueue.Status.Failed(entity.errorMessage)
                        else -> ImageUploadQueue.Status.Pending
                    },
                    errorMessage = entity.errorMessage,
                    stackTrace = entity.stackTrace,
                    createdAt = entity.createdAt,
                    workManagerId = entity.workManagerId,
                )
            }
        }
    }

    override suspend fun cancel(itemId: String) {
        val entity = dao.getById(itemId) ?: return
        val workManagerId = entity.workManagerId
        dao.deleteById(itemId)
        withContext(Dispatchers.IO) {
            rawImageBytesFile(context, itemId).delete()
            previewBytesFile(context, itemId).delete()
        }
        if (workManagerId != null) {
            WorkManager.getInstance(context).cancelWorkById(UUID.fromString(workManagerId))
        }
    }

    internal companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_UPLOADING = "UPLOADING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"

        fun uniqueWorkName(moneyUsageId: MoneyUsageId): String = "image_upload_${moneyUsageId.id}"
    }
}
