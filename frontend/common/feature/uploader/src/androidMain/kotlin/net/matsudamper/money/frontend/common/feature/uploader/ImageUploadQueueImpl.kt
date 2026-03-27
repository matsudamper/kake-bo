package net.matsudamper.money.frontend.common.feature.uploader

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.matsudamper.money.element.MoneyUsageId

internal class ImageUploadQueueImpl(
    private val context: Context,
    private val dao: ImageUploadDao,
) : ImageUploadQueue {

    override fun observeItems(moneyUsageId: MoneyUsageId): Flow<List<ImageUploadQueue.QueueItem>> {
        return dao.observeByMoneyUsageId(moneyUsageId.id).map { entities ->
            entities.map { entity ->
                ImageUploadQueue.QueueItem(
                    id = entity.id,
                    previewBytes = entity.previewBytes,
                    status = when (entity.status) {
                        STATUS_UPLOADING -> ImageUploadQueue.Status.Uploading
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
        dao.insert(
            ImageUploadEntity(
                id = id,
                moneyUsageId = moneyUsageId.id,
                rawImageBytes = rawImageBytes,
                previewBytes = previewBytes,
                status = STATUS_PENDING,
                workManagerId = null,
                errorMessage = null,
                createdAt = System.currentTimeMillis(),
            ),
        )
        val request = OneTimeWorkRequestBuilder<ImageUploadWorker>()
            .setInputData(workDataOf(ImageUploadWorker.KEY_RECORD_ID to id))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(moneyUsageId),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }

    override suspend fun retry(itemId: String) {
        val entity = dao.getById(itemId) ?: return
        dao.updateStatusWithError(itemId, STATUS_PENDING, null)
        val request = OneTimeWorkRequestBuilder<ImageUploadWorker>()
            .setInputData(workDataOf(ImageUploadWorker.KEY_RECORD_ID to itemId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(MoneyUsageId(entity.moneyUsageId)),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }

    internal companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_UPLOADING = "UPLOADING"
        const val STATUS_FAILED = "FAILED"

        fun uniqueWorkName(moneyUsageId: MoneyUsageId): String = "image_upload_${moneyUsageId.id}"
    }
}
