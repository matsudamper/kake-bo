package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.UserId

interface UserImageRepository {
    data class SaveImageResult(
        val imageId: ImageId,
        val displayId: String,
    )

    fun saveImage(
        userId: UserId,
        displayId: String,
        relativePath: String,
    ): SaveImageResult?

    fun getRelativePathByDisplayId(
        userId: UserId,
        displayId: String,
    ): String?

    fun exists(
        userId: UserId,
        imageId: ImageId,
    ): Boolean

    fun getDisplayIdsByImageIds(
        userId: UserId,
        imageIds: List<ImageId>,
    ): Map<ImageId, String>
}
