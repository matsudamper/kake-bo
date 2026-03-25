package net.matsudamper.money.frontend.common.base

import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.MoneyUsageId

public interface MoneyUsageImageUploadScheduler {
    public suspend fun scheduleUploadAndLink(
        bytes: ByteArray,
        contentType: String?,
        moneyUsageId: MoneyUsageId,
        currentImageIds: List<ImageId>,
    ): Boolean
}
