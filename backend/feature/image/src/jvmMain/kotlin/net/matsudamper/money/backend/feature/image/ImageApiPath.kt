package net.matsudamper.money.backend.feature.image

object ImageApiPath {
    fun imageV1ByDisplayId(displayId: String): String {
        return "/api/image/v1/$displayId"
    }

    fun imageV1AbsoluteByDisplayId(
        domain: String,
        displayId: String,
    ): String {
        return "https://$domain${imageV1ByDisplayId(displayId)}"
    }
}
