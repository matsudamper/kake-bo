package net.matsudamper.money.backend.image

import kotlinx.serialization.json.Json
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import net.matsudamper.money.backend.app.interfaces.AdminImageRepository
import net.matsudamper.money.backend.app.interfaces.ImageStorageGateway
import net.matsudamper.money.backend.app.interfaces.UserImageRepository
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.backend.feature.image.ImageApiPath
import net.matsudamper.money.backend.feature.session.KtorCookieManager
import net.matsudamper.money.backend.feature.session.UserSessionManagerImpl
import net.matsudamper.money.element.UserId
import net.matsudamper.money.image.ImageUploadImageResponse

internal fun Route.getImage(
    diContainer: DiContainer,
) {
    get(ImageApiPath.imageV1ByDisplayId("{displayId}")) {
        val userId = call.requireUserId(diContainer = diContainer) ?: return@get
        val displayId = run {
            val displayId = call.parameters["displayId"]
            if (displayId == null) {
                call.respondApiError(
                    status = HttpStatusCode.BadRequest,
                    message = "InvalidImageId",
                )
                return@get
            }
            displayId
        }

        val imageData = diContainer.createUserImageRepository().getImageDataByDisplayId(
            userId = userId,
            displayId = displayId,
        )?.toRoutingImageData(userId)

        if (imageData == null) {
            call.respondApiError(
                status = HttpStatusCode.NotFound,
                message = "NotFound",
            )
            return@get
        }

        call.respondImageByDisplayId(
            diContainer = diContainer,
            imageData = imageData,
            displayId = displayId,
            purpose = ImageStorageGateway.Purpose.USER,
        )
    }

    get(ImageApiPath.adminImageV1ByDisplayId("{displayId}")) {
        val isAuthorized = call.requireAdminAuthorization(diContainer = diContainer)
        if (!isAuthorized) return@get
        val displayId = run {
            val displayId = call.parameters["displayId"]
            if (displayId == null) {
                call.respondApiError(
                    status = HttpStatusCode.BadRequest,
                    message = "InvalidImageId",
                )
                return@get
            }
            displayId
        }
        val imageData = diContainer.createAdminImageRepository().getImageDataByDisplayId(displayId)
            ?.toRoutingImageData()

        if (imageData == null) {
            call.respondApiError(
                status = HttpStatusCode.NotFound,
                message = "NotFound",
            )
            return@get
        }

        call.respondImageByDisplayId(
            diContainer = diContainer,
            imageData = imageData,
            displayId = displayId,
            purpose = ImageStorageGateway.Purpose.ADMIN,
        )
    }
}

private suspend fun ApplicationCall.requireUserId(
    diContainer: DiContainer,
) = UserSessionManagerImpl(
    cookieManager = KtorCookieManager(call = this),
    userSessionRepository = diContainer.createUserSessionRepository(),
).verifyUserSession() ?: run {
    respondApiError(
        status = HttpStatusCode.Unauthorized,
        message = "Unauthorized",
    )
    null
}

private suspend fun ApplicationCall.requireAdminAuthorization(
    diContainer: DiContainer,
): Boolean {
    val cookieManager = KtorCookieManager(call = this)
    val adminSessionId = cookieManager.getAdminSessionId()
    if (adminSessionId == null) {
        respondApiError(
            status = HttpStatusCode.Unauthorized,
            message = "Unauthorized",
        )
        return false
    }

    val adminSession = diContainer.createAdminUserSessionRepository().verifySession(adminSessionId)
    if (adminSession == null) {
        respondApiError(
            status = HttpStatusCode.Unauthorized,
            message = "Unauthorized",
        )
        return false
    }

    cookieManager.setAdminSession(
        idValue = adminSession.adminSessionId.id,
        expires = adminSession.expire.atOffset(java.time.ZoneOffset.UTC),
    )
    return true
}

private suspend fun ApplicationCall.respondImageByDisplayId(
    diContainer: DiContainer,
    imageData: RoutingImageData,
    displayId: String,
    purpose: ImageStorageGateway.Purpose,
) {
    val gateway = diContainer.createReadImageStorageGateway(imageData.storageType)
    val readRequest = ImageStorageGateway.ReadRequest(
        relativePath = imageData.relativePath,
        displayId = displayId,
        userId = imageData.userId,
        domain = requireNotNull(ServerEnv.domain) { "DOMAIN が未設定です" },
        purpose = purpose,
    )
    when (val result = gateway.read(readRequest)) {
        null -> {
            respondApiError(
                status = HttpStatusCode.NotFound,
                message = "NotFound",
            )
        }
        is ImageStorageGateway.ReadResult.Stream -> {
            val responseContentType = runCatching {
                ContentType.parse(imageData.contentType)
            }.getOrDefault(ContentType.Application.OctetStream)

            respondOutputStream(contentType = responseContentType) {
                result.inputStream.use { it.copyTo(this) }
            }
        }
        is ImageStorageGateway.ReadResult.RedirectUrl -> {
            respondRedirect(url = result.url, permanent = false)
        }
    }
}

private fun UserImageRepository.ImageData.toRoutingImageData(userId: UserId) = RoutingImageData(
    relativePath = relativePath,
    contentType = contentType,
    storageType = storageType,
    userId = userId,
)

private fun AdminImageRepository.ImageData.toRoutingImageData() = RoutingImageData(
    relativePath = relativePath,
    contentType = contentType,
    storageType = storageType,
    userId = userId,
)

private data class RoutingImageData(
    val relativePath: String,
    val contentType: String,
    val storageType: UserImageRepository.StorageType,
    val userId: UserId,
)

private suspend fun ApplicationCall.respondApiError(
    status: HttpStatusCode,
    message: String,
) {
    respondText(
        status = status,
        contentType = ContentType.Application.Json,
        text = Json.encodeToString(
            ImageUploadImageResponse(
                error = mapOf("message" to message),
            ),
        ),
    )
}
