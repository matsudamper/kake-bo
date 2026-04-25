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
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.image.SelectedImage
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.MoneyUsageScreenQuery
import net.matsudamper.money.frontend.graphql.MoneyUsageScreenUpdateUsageMutation
import net.matsudamper.money.frontend.graphql.type.UpdateUsageQuery
import org.w3c.dom.Worker

private const val STATUS_PENDING = "PENDING"
private const val STATUS_UPLOADING = "UPLOADING"
private const val STATUS_COMPLETED = "COMPLETED"
private const val STATUS_FAILED = "FAILED"
private const val TAG = "ImageUploadQueueJsImpl"

public class ImageUploadQueueJsImpl private constructor(
    private val dao: ImageUploadRoomDao,
    private val graphqlClient: GraphqlClient,
    private val imageUploadClient: ImageUploadClient,
    private val localStorage: ImageUploadLocalStorage,
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
                    previewBytes = localStorage.readPreview(entity.id),
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

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun enqueue(
        moneyUsageId: MoneyUsageId,
        selectedImage: SelectedImage,
    ) {
        val id = Uuid.random().toString()
        val imageBytes = selectedImage.bytes
        if (imageBytes != null) {
            localStorage.writeRawImage(id, imageBytes)
            localStorage.writePreview(id, imageBytes)
        }
        dao.insert(
            ImageUploadRoomEntity(
                id = id,
                moneyUsageId = moneyUsageId.id,
                status = STATUS_PENDING,
                imageSourceUri = null,
                workManagerId = null,
                errorMessage = null,
                stackTrace = null,
                contentType = selectedImage.contentType,
                createdAt = js("Date.now()").unsafeCast<Double>().toLong(),
            ),
        )
        triggerNext(moneyUsageId)
    }

    override suspend fun retry(itemId: String) {
        val entity = dao.getById(itemId) ?: return
        dao.updateStatusWithError(itemId, STATUS_PENDING, null, null)
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
        localStorage.deleteImages(itemId)
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
        val entity = dao.getById(itemId) ?: run {
            dao.updateStatusWithError(itemId, STATUS_FAILED, "レコードが見つかりません", null)
            return
        }
        val rawImageBytes = localStorage.readRawImage(itemId) ?: run {
            dao.updateStatusWithError(itemId, STATUS_FAILED, "画像データが見つかりません", null)
            return
        }

        val uploadedResult = runCatching {
            imageUploadClient.upload(
                bytes = rawImageBytes,
                contentType = entity.contentType,
            ) ?: throw IllegalStateException("upload returned null")
        }
        val uploaded = uploadedResult.onFailure { Logger.e(TAG, it) }.getOrNull()
        if (uploaded == null) {
            dao.updateStatusWithError(
                itemId,
                STATUS_FAILED,
                "アップロードに失敗しました",
                uploadedResult.exceptionOrNull()?.stackTraceToString(),
            )
            return
        }

        val moneyUsageId = MoneyUsageId(entity.moneyUsageId)
        val queryResult = runCatching {
            graphqlClient.apolloClient
                .query(MoneyUsageScreenQuery(id = moneyUsageId))
                .execute()
                .also { response ->
                    if (response.hasErrors()) {
                        throw IllegalStateException("GraphQL query errors: ${response.errors}")
                    }
                }
                .data?.user?.moneyUsage?.moneyUsageScreenMoneyUsage?.images
                ?.map { it.id }
        }
        val currentImageIds = queryResult.onFailure { Logger.e(TAG, it) }.getOrNull()

        val updatedImageIds = ((currentImageIds ?: listOf()) + uploaded.imageId)
            .distinctBy { it.value }

        val mutationResult = runCatching {
            val response = graphqlClient.apolloClient
                .mutation(
                    MoneyUsageScreenUpdateUsageMutation(
                        query = UpdateUsageQuery(
                            id = moneyUsageId,
                            imageIds = Optional.present(updatedImageIds),
                        ),
                    ),
                )
                .execute()
            if (response.hasErrors()) {
                throw IllegalStateException("GraphQL mutation errors: ${response.errors}")
            }
            response.data?.userMutation?.updateUsage != null
        }

        if (mutationResult.getOrDefault(false) == false) {
            dao.updateStatusWithError(
                itemId,
                STATUS_FAILED,
                "使用用途の更新に失敗しました",
                mutationResult.exceptionOrNull()?.stackTraceToString(),
            )
            return
        }

        dao.updateStatus(itemId, STATUS_COMPLETED)
        localStorage.deleteImages(itemId)
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

    public companion object {
        public fun create(
            graphqlClient: GraphqlClient,
            imageUploadClient: ImageUploadClient,
        ): ImageUploadQueueJsImpl {
            val worker = Worker(js("""new URL("@androidx/sqlite-web-worker/worker.js", import.meta.url)"""))
            val db = Room.inMemoryDatabaseBuilder<ImageUploadRoomDatabase>()
                .setDriver(WebWorkerSQLiteDriver(worker))
                .setQueryCoroutineContext(Dispatchers.Default)
                .build()
            return ImageUploadQueueJsImpl(
                dao = db.dao(),
                graphqlClient = graphqlClient,
                imageUploadClient = imageUploadClient,
                localStorage = ImageUploadLocalStorageJsImpl(),
            )
        }
    }
}
