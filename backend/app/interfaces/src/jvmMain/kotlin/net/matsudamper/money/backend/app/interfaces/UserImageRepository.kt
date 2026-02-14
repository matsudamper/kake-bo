package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.UserId

interface UserImageRepository {
    fun saveImage(
        userId: UserId,
        imageId: ImageId,
        relativePath: String,
    ): Boolean

    fun getRelativePath(
        userId: UserId,
        imageId: ImageId,
    ): String?

    fun exists(
        userId: UserId,
        imageId: ImageId,
    ): Boolean
}
