package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.UserId

interface UsageImageRelationDao {
    /**
     * 画像を末尾に紐付ける。既に紐付いている場合は何もしない
     * @return 成功したかどうか
     */
    fun add(
        userId: UserId,
        moneyUsageId: MoneyUsageId,
        imageId: ImageId,
    ): Boolean

    /**
     * 旧画像の位置に新画像を紐付け、旧画像がどこからも紐付いていなければ削除する。旧画像が紐付いていない場合は新画像を末尾に紐付ける
     * @return 成功したかどうか
     */
    fun replace(
        userId: UserId,
        moneyUsageId: MoneyUsageId,
        oldImageId: ImageId,
        newImageId: ImageId,
    ): Boolean
}
