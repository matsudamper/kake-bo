package net.matsudamper.money.frontend.common.feature.uploader

import kotlinx.coroutines.flow.Flow
import net.matsudamper.money.element.MoneyUsageId

public interface ImageUploadQueue {
    public sealed class Status {
        public data object Pending : Status()

        public data object Uploading : Status()

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
        val createdAt: Long,
        val workManagerId: String?,
    )

    public fun observeItems(moneyUsageId: MoneyUsageId): Flow<List<QueueItem>>

    public suspend fun enqueue(
        moneyUsageId: MoneyUsageId,
        rawImageBytes: ByteArray,
        previewBytes: ByteArray?,
    )

    public suspend fun retry(itemId: String)

    public suspend fun cancel(itemId: String)

    public suspend fun getPagedDebugItems(offset: Int, limit: Int): List<DebugItem>

    public suspend fun countAllDebugItems(): Int
}
