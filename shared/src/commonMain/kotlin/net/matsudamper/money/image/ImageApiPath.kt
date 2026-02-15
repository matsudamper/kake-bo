package net.matsudamper.money.image

import net.matsudamper.money.element.ImageId

public object ImageApiPath {
    public const val uploadV1: String = "/api/image/upload/v1"

    public fun imageV1(imageId: ImageId): String {
        return imageV1(imageId.value)
    }

    public fun imageV1(imageId: String): String {
        return "/api/image/v1/$imageId"
    }
}
