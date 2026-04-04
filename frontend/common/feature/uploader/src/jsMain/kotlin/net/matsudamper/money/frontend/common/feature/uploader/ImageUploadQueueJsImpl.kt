package net.matsudamper.money.frontend.common.feature.uploader

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.matsudamper.money.element.MoneyUsageId

public class ImageUploadQueueJsImpl : ImageUploadQueue {
    override fun observeItems(moneyUsageId: MoneyUsageId): Flow<List<ImageUploadQueue.QueueItem>> {
        return flowOf(listOf())
    }

    override suspend fun enqueue(
        moneyUsageId: MoneyUsageId,
        rawImageBytes: ByteArray,
        previewBytes: ByteArray?,
    ) {
        // JSではWorkManagerを使用しないため何もしない
    }

    override suspend fun retry(itemId: String) {
        // JSではWorkManagerを使用しないため何もしない
    }

    override suspend fun cancel(itemId: String) {
        // JSではWorkManagerを使用しないため何もしない
    }
}
