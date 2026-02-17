package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.UserId

interface DeleteUsageImageRelationDao {
    /**
     * @return 削除に成功したかどうか
     */
    fun delete(
        userId: UserId,
        moneyUsageId: MoneyUsageId,
        imageId: ImageId,
    ): Boolean
}
