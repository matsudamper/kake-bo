package net.matsudamper.money.frontend.common.feature.uploader

import kotlinx.coroutines.flow.Flow
import net.matsudamper.money.element.MoneyUsageId

public interface ImageUploadQueue {
    public sealed class Status {
        public data object Pending : Status()

        public data object Uploading : Status()

        public data object Completed : Status()

        public data class Failed(val message: String?) : Status()
    }

    public data class QueueItem(
        val id: String,
        val previewBytes: ByteArray?,
        val status: Status,
    )

    public data class DebugItem(
        val id: String,
        val moneyUsageId: Int,
        val status: Status,
        val errorMessage: String?,
        val stackTrace: String?,
        val createdAt: Long,
        val workManagerId: String?,
    )

    public fun observeItems(moneyUsageId: MoneyUsageId): Flow<List<QueueItem>>

    public fun observeAllDebugItems(): Flow<List<DebugItem>>

    public suspend fun enqueue(
        moneyUsageId: MoneyUsageId,
        rawImageBytes: ByteArray,
        previewBytes: ByteArray?,
        contentType: String?,
    )

    public suspend fun retry(itemId: String)

    public suspend fun cancel(itemId: String)
}
