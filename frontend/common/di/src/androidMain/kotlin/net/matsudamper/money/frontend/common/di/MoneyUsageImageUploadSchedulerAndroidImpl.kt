package net.matsudamper.money.frontend.common.di

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.base.MoneyUsageImageUploadScheduler

internal class MoneyUsageImageUploadSchedulerAndroidImpl(
    private val context: Context,
) : MoneyUsageImageUploadScheduler {

    override suspend fun scheduleUploadAndLink(
        bytes: ByteArray,
        contentType: String?,
        moneyUsageId: MoneyUsageId,
        currentImageIds: List<ImageId>,
    ): Boolean {
        if (bytes.isEmpty()) return false

        val tempFile = withContext(Dispatchers.IO) {
            File(context.cacheDir, "img_upload_${UUID.randomUUID()}").also {
                it.writeBytes(bytes)
            }
        }

        val inputData = workDataOf(
            ImageUploadWorker.KEY_FILE_PATH to tempFile.absolutePath,
            ImageUploadWorker.KEY_CONTENT_TYPE to (contentType ?: "application/octet-stream"),
            ImageUploadWorker.KEY_MONEY_USAGE_ID to moneyUsageId.id,
            ImageUploadWorker.KEY_CURRENT_IMAGE_IDS to currentImageIds.map { it.value }.toIntArray(),
        )

        val workRequest = OneTimeWorkRequestBuilder<ImageUploadWorker>()
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(workRequest)

        return workManager.getWorkInfoByIdFlow(workRequest.id)
            .first { it?.state?.isFinished == true }
            ?.state == WorkInfo.State.SUCCEEDED
    }
}
