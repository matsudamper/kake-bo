package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.UserId

interface UserImageRepository {
    enum class StorageType { LOCAL, S3; }

    data class ImageData(
        val relativePath: String,
        val contentType: String,
        val storageType: StorageType,
    )

    data class ImageInfo(
        val displayId: String,
        val relativePath: String,
        val storageType: StorageType,
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

    fun getDisplayIdsByImageIds(
        userId: UserId,
        imageIds: List<ImageId>,
    ): Map<ImageId, String>

    fun getImageInfoByImageIds(
        userId: UserId,
        imageIds: List<ImageId>,
    ): Map<ImageId, ImageInfo>
}
