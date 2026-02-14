package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.UserId

interface UserImageRepository {
    fun saveImage(
        userId: UserId,
        imageHash: String,
        relativePath: String,
    ): Boolean

    fun getRelativePath(
        userId: UserId,
        imageHash: String,
    ): String?
}
