package net.matsudamper.money.backend.feature.image

object LocalImageApiPath {
    fun imageV1ByDisplayId(displayId: String): String {
        return "/api/image/v1/$displayId"
    }

    fun adminImageV1ByDisplayId(displayId: String): String {
        return "/api/admin/image/v1/$displayId"
    }

    fun adminImageV1AbsoluteByDisplayId(
        domain: String,
        displayId: String,
    ): String {
        return "https://$domain${adminImageV1ByDisplayId(displayId)}"
    }
}
