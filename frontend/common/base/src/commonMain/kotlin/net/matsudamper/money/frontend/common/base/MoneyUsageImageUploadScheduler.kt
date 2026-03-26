package net.matsudamper.money.frontend.common.base

import kotlinx.coroutines.flow.Flow
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.MoneyUsageId

public interface MoneyUsageImageUploadScheduler {
    public suspend fun scheduleUploadAndLink(
        bytes: ByteArray,
        contentType: String?,
        moneyUsageId: MoneyUsageId,
        currentImageIds: List<ImageId>,
    ): Boolean

    public fun getActiveUploadCount(moneyUsageId: MoneyUsageId): Flow<Int>
}
