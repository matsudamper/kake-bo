package net.matsudamper.money.backend.graphql.resolver

import net.matsudamper.money.backend.lib.CursorParser
import net.matsudamper.money.element.ImageId

internal data class AdminUnlinkedImagesCursor(
    val imageId: ImageId,
) {
    fun toCursorString(): String {
        return CursorParser.createToString(
            mapOf(IMAGE_ID_KEY to imageId.value.toString()),
        )
    }

    companion object {
        private const val IMAGE_ID_KEY = "imageId"

        fun fromString(value: String): AdminUnlinkedImagesCursor {
            return AdminUnlinkedImagesCursor(
                imageId = ImageId(CursorParser.parseFromString(value)[IMAGE_ID_KEY]!!.toInt()),
            )
        }
    }
}
