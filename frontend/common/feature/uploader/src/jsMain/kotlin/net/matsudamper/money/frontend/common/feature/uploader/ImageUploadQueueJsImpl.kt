package net.matsudamper.money.frontend.common.feature.uploader

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo.api.Optional
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.base.ImageUploadClient
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.MoneyUsageScreenQuery
import net.matsudamper.money.frontend.graphql.MoneyUsageScreenUpdateUsageMutation
import net.matsudamper.money.frontend.graphql.type.UpdateUsageQuery

public class ImageUploadQueueJsImpl(
    private val graphqlClient: GraphqlClient,
    private val imageUploadClient: ImageUploadClient,
) : ImageUploadQueue {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val queueEntriesFlow = MutableStateFlow<List<QueueEntry>>(emptyList())
    private val activeJobs = mutableMapOf<Int, Job>()
    private val activeItemIds = mutableMapOf<Int, String>()

    override fun observeItems(moneyUsageId: MoneyUsageId): Flow<List<ImageUploadQueue.QueueItem>> {
        return queueEntriesFlow.map { entries ->
            entries
                .filter { it.moneyUsageId == moneyUsageId }
                .map { entry ->
                    ImageUploadQueue.QueueItem(
                        id = entry.id,
                        previewBytes = entry.previewBytes,
                        status = entry.status,
                    )
                }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun enqueue(
        moneyUsageId: MoneyUsageId,
        rawImageBytes: ByteArray,
        previewBytes: ByteArray?,
    ) {
        queueEntriesFlow.update { entries ->
            entries + QueueEntry(
                id = Uuid.random().toString(),
                moneyUsageId = moneyUsageId,
                rawImageBytes = rawImageBytes,
                previewBytes = previewBytes,
                status = ImageUploadQueue.Status.Pending,
            )
        }
        triggerNext(moneyUsageId)
    }

    override suspend fun retry(itemId: String) {
        val entry = queueEntriesFlow.value.firstOrNull { it.id == itemId } ?: return
        updateStatus(itemId, ImageUploadQueue.Status.Pending)
        triggerNext(entry.moneyUsageId)
    }

    override suspend fun cancel(itemId: String) {
        val entry = queueEntriesFlow.value.firstOrNull { it.id == itemId } ?: return
        val moneyUsageKey = entry.moneyUsageId.id
        if (activeItemIds[moneyUsageKey] == itemId) {
            activeJobs.remove(moneyUsageKey)?.cancel()
            activeItemIds.remove(moneyUsageKey)
        }
        queueEntriesFlow.update { entries ->
            entries.filterNot { it.id == itemId }
        }
        triggerNext(entry.moneyUsageId)
    }

    private fun triggerNext(moneyUsageId: MoneyUsageId) {
        val moneyUsageKey = moneyUsageId.id
        if (activeJobs[moneyUsageKey]?.isActive == true) return

        val nextEntry = queueEntriesFlow.value.firstOrNull {
            it.moneyUsageId == moneyUsageId && it.status is ImageUploadQueue.Status.Pending
        } ?: run {
            activeJobs.remove(moneyUsageKey)
            activeItemIds.remove(moneyUsageKey)
            return
        }

        activeItemIds[moneyUsageKey] = nextEntry.id
        activeJobs[moneyUsageKey] = scope.launch {
            try {
                processEntry(nextEntry.id)
            } finally {
                activeJobs.remove(moneyUsageKey)
                activeItemIds.remove(moneyUsageKey)
                triggerNext(moneyUsageId)
            }
        }
    }

    private suspend fun processEntry(itemId: String) {
        updateStatus(itemId, ImageUploadQueue.Status.Uploading)
        val entry = queueEntriesFlow.value.firstOrNull { it.id == itemId } ?: return

        val uploadResult = imageUploadClient.upload(
            bytes = entry.rawImageBytes,
            contentType = null,
        )
        if (uploadResult == null) {
            updateStatus(itemId, ImageUploadQueue.Status.Failed("アップロードに失敗しました"))
            return
        }

        val currentImageIds = runCatching {
            graphqlClient.apolloClient
                .query(MoneyUsageScreenQuery(id = entry.moneyUsageId))
                .execute()
                .data?.user?.moneyUsage?.moneyUsageScreenMoneyUsage?.images
                ?.map { it.id }
        }.getOrNull()

        val updatedImageIds = ((currentImageIds ?: emptyList()) + uploadResult.imageId)
            .distinctBy { it.value }

        val isSuccess = runCatching {
            graphqlClient.apolloClient
                .mutation(
                    MoneyUsageScreenUpdateUsageMutation(
                        query = UpdateUsageQuery(
                            id = entry.moneyUsageId,
                            imageIds = Optional.present(updatedImageIds),
                        ),
                    ),
                )
                .execute()
                .data?.userMutation?.updateUsage != null
        }.getOrDefault(false)

        if (!isSuccess) {
            updateStatus(itemId, ImageUploadQueue.Status.Failed("使用用途の更新に失敗しました"))
            return
        }

        queueEntriesFlow.update { entries ->
            entries.filterNot { it.id == itemId }
        }
    }

    private fun updateStatus(
        itemId: String,
        status: ImageUploadQueue.Status,
    ) {
        queueEntriesFlow.update { entries ->
            entries.map { entry ->
                if (entry.id == itemId) {
                    entry.copy(status = status)
                } else {
                    entry
                }
            }
        }
    }

    private data class QueueEntry(
        val id: String,
        val moneyUsageId: MoneyUsageId,
        val rawImageBytes: ByteArray,
        val previewBytes: ByteArray?,
        val status: ImageUploadQueue.Status,
    )
}
