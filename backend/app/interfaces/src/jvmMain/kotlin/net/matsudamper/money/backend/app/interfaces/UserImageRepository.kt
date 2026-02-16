package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.UserId

interface UserImageRepository {
    data class ImageData(
        val relativePath: String,
        val contentType: String,
    )

    fun saveImage(
        userId: UserId,
        displayId: String,
        relativePath: String,
        contentType: String,
    ): ImageId?

    fun markImageAsUploaded(
        userId: UserId,
        imageId: ImageId,
    )

    fun deleteImage(
        userId: UserId,
        imageId: ImageId,
    )

    fun getImageDataByDisplayId(
        userId: UserId,
        displayId: String,
    ): ImageData?

    fun exists(
        userId: UserId,
        imageId: ImageId,
    ): Boolean

    fun getDisplayIdsByImageIds(
        userId: UserId,
        imageIds: List<ImageId>,
    ): Map<ImageId, String>

    fun getRelativePath(
        userId: UserId,
        imageId: ImageId,
    ): String?

    fun countImageUsageRelations(
        userId: UserId,
        imageId: ImageId,
    ): Int

    fun deleteImageUsageRelation(
        userId: UserId,
        moneyUsageId: MoneyUsageId,
        imageId: ImageId,
    )
}
