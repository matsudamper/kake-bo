package net.matsudamper.money.backend.feature.image

import net.matsudamper.money.element.ImageId

object ImageApiPath {
    fun imageV1(imageId: ImageId): String {
        return imageV1(imageId.value)
    }

    fun imageV1(imageId: String): String {
        return "/api/image/v1/$imageId"
    }
}
