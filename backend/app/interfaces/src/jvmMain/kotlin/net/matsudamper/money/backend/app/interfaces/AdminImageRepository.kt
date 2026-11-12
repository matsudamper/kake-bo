package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.UserId

interface AdminImageRepository {
    data class ImageData(
        val relativePath: String,
        val contentType: String,
        val userId: UserId,
        val storageType: UserImageRepository.StorageType,
    )

    fun getUnlinkedImages(
        size: Int,
        cursor: Cursor?,
    ): Result

    fun deleteImages(imageIds: List<ImageId>): Boolean

    fun countUnlinkedImages(): Int

    fun getImageDataByDisplayId(displayId: String): ImageData?

    /** S3 に保存済みでサムネイルが無い画像を imageId の昇順で返す */
    fun getS3ImagesWithoutThumbnail(
        size: Int,
        afterImageId: ImageId?,
    ): List<ImageWithoutThumbnail>

    fun updateThumbnailPath(
        imageId: ImageId,
        thumbnailRelativePath: String,
    )

    data class ImageWithoutThumbnail(
        val imageId: ImageId,
        val userId: UserId,
        val relativePath: String,
    )

    data class Cursor(
        val imageId: ImageId,
    )

    data class Result(
        val items: List<Item>,
        val cursor: Cursor?,
    )

    data class Item(
        val imageId: ImageId,
        val displayId: String,
        val userId: UserId,
        val userName: String,
    )
}
