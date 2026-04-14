package net.matsudamper.money.frontend.common.feature.uploader

import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.apollographql.apollo.api.Optional
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.base.ImageUploadClient
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.MoneyUsageScreenQuery
import net.matsudamper.money.frontend.graphql.MoneyUsageScreenUpdateUsageMutation
import net.matsudamper.money.frontend.graphql.type.UpdateUsageQuery
import org.w3c.dom.Worker

private const val STATUS_PENDING = "PENDING"
private const val STATUS_UPLOADING = "UPLOADING"
private const val STATUS_FAILED = "FAILED"

public class ImageUploadQueueJsImpl private constructor(
    private val dao: ImageUploadRoomDao,
    private val graphqlClient: GraphqlClient,
    private val imageUploadClient: ImageUploadClient,
) : ImageUploadQueue {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activeJobs = mutableMapOf<Int, Job>()
    private val activeItemIds = mutableMapOf<Int, String>()

    init {
        scope.launch {
            // 前回セッションでUPLOADING状態のままになっていたアイテムをPENDINGに戻す
            dao.resetUploadingToPending()
            val pendingIds = dao.getDistinctMoneyUsageIdsWithPendingItems()
            pendingIds.forEach { moneyUsageId ->
                triggerNext(MoneyUsageId(moneyUsageId))
            }
        }
    }

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

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun enqueue(
        moneyUsageId: MoneyUsageId,
        rawImageBytes: ByteArray,
        previewBytes: ByteArray?,
    ) {
        val id = Uuid.random().toString()
        dao.insert(
            ImageUploadRoomEntity(
                id = id,
                moneyUsageId = moneyUsageId.id,
                status = STATUS_PENDING,
                workManagerId = null,
                errorMessage = null,
                createdAt = js("Date.now()").unsafeCast<Double>().toLong(),
                rawImageBytes = rawImageBytes,
                previewBytes = previewBytes,
            ),
        )
        triggerNext(moneyUsageId)
    }

    override suspend fun retry(itemId: String) {
        val entity = dao.getById(itemId) ?: return
        dao.updateStatusWithError(itemId, STATUS_PENDING, null)
        triggerNext(MoneyUsageId(entity.moneyUsageId))
    }

    override suspend fun cancel(itemId: String) {
        val entity = dao.getById(itemId) ?: return
        val moneyUsageKey = entity.moneyUsageId
        if (activeItemIds[moneyUsageKey] == itemId) {
            activeJobs.remove(moneyUsageKey)?.cancel()
            activeItemIds.remove(moneyUsageKey)
        }
        dao.deleteById(itemId)
        triggerNext(MoneyUsageId(entity.moneyUsageId))
    }

    private fun triggerNext(moneyUsageId: MoneyUsageId) {
        val moneyUsageKey = moneyUsageId.id
        if (activeJobs[moneyUsageKey]?.isActive == true) return

        activeJobs[moneyUsageKey] = scope.launch {
            val nextEntry = dao.getOldestPendingByMoneyUsageId(moneyUsageId.id)
            if (nextEntry == null) {
                activeJobs.remove(moneyUsageKey)
                activeItemIds.remove(moneyUsageKey)
                return@launch
            }
            activeItemIds[moneyUsageKey] = nextEntry.id
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
        dao.updateStatus(itemId, STATUS_UPLOADING)
        val entity = dao.getById(itemId) ?: return
        val rawImageBytes = entity.rawImageBytes ?: return

        val uploadResult = imageUploadClient.upload(
            bytes = rawImageBytes,
            contentType = null,
        )
        if (uploadResult == null) {
            dao.updateStatusWithError(itemId, STATUS_FAILED, "アップロードに失敗しました")
            return
        }

        val moneyUsageId = MoneyUsageId(entity.moneyUsageId)
        val currentImageIds = runCatching {
            graphqlClient.apolloClient
                .query(MoneyUsageScreenQuery(id = moneyUsageId))
                .execute()
                .data?.user?.moneyUsage?.moneyUsageScreenMoneyUsage?.images
                ?.map { it.id }
        }.getOrNull()

        val updatedImageIds = ((currentImageIds ?: listOf()) + uploadResult.imageId)
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
            dao.updateStatusWithError(itemId, STATUS_FAILED, "使用用途の更新に失敗しました")
            return
        }

        dao.deleteById(itemId)
    }

    override suspend fun getPagedDebugItems(offset: Int, limit: Int): List<ImageUploadQueue.DebugItem> {
        return dao.getPagedAllByCreatedAtDesc(limit = limit, offset = offset).map { entity ->
            ImageUploadQueue.DebugItem(
                id = entity.id,
                moneyUsageId = entity.moneyUsageId,
                status = when (entity.status) {
                    STATUS_UPLOADING -> ImageUploadQueue.Status.Uploading
                    STATUS_FAILED -> ImageUploadQueue.Status.Failed(entity.errorMessage)
                    else -> ImageUploadQueue.Status.Pending
                },
                errorMessage = entity.errorMessage,
                createdAt = entity.createdAt,
                workManagerId = entity.workManagerId,
            )
        }
    }

    override suspend fun countAllDebugItems(): Int {
        return dao.countAll()
    }

    public companion object {
        public fun create(
            graphqlClient: GraphqlClient,
            imageUploadClient: ImageUploadClient,
        ): ImageUploadQueueJsImpl {
            val worker = Worker(js("""new URL("@androidx/sqlite-web-worker/worker.js", import.meta.url)"""))
            val db = Room.databaseBuilder<ImageUploadRoomDatabase>("image_upload_queue.db")
                .setDriver(WebWorkerSQLiteDriver(worker))
                .setQueryCoroutineContext(Dispatchers.Default)
                .build()
            return ImageUploadQueueJsImpl(
                dao = db.dao(),
                graphqlClient = graphqlClient,
                imageUploadClient = imageUploadClient,
            )
        }
    }
}
