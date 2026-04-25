package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.UserId

interface AdminImageRepository {
    data class ImageData(
        val relativePath: String,
        val contentType: String,
    )

    fun getUnlinkedImages(
        size: Int,
        cursor: Cursor?,
    ): Result

    fun countUnlinkedImages(): Int

    fun getImageDataByDisplayId(displayId: String): ImageData?

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
