package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.UserId

interface UserImageRepository {
    enum class StorageType { LOCAL, S3; }

    data class ImageData(
        val relativePath: String,
        val thumbnailRelativePath: String?,
        val contentType: String,
        val storageType: StorageType,
    )

    data class ImageSummary(
        val displayId: String,
        val hasThumbnail: Boolean,
    )

    fun saveImage(
        userId: UserId,
        displayId: String,
        relativePath: String,
        contentType: String,
        storageType: StorageType,
    ): ImageId?

    fun markImageAsUploaded(
        userId: UserId,
        imageId: ImageId,
        thumbnailRelativePath: String?,
    )

    fun deleteReserveImage(
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

    fun getImageSummariesByImageIds(
        userId: UserId,
        imageIds: List<ImageId>,
    ): Map<ImageId, ImageSummary>
}
