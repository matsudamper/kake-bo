package net.matsudamper.money.backend.image

import java.io.InputStream
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
import net.matsudamper.money.backend.feature.image.LocalImageApiPath
import net.matsudamper.money.backend.feature.imagestoragelocal.LocalImageStorageGateway
import net.matsudamper.money.backend.feature.session.KtorCookieManager
import net.matsudamper.money.backend.feature.session.UserSessionManagerImpl
import net.matsudamper.money.image.ImageUploadImageResponse

internal fun Route.getImage(
    diContainer: DiContainer,
) {
    get(LocalImageApiPath.imageV1ByDisplayId("{displayId}")) {
        val userId = call.requireUserId(diContainer = diContainer) ?: return@get

        call.respondImageByDisplayId(
            diContainer = diContainer,
            getImageData = { displayId ->
                diContainer.createUserImageRepository().getImageDataByDisplayId(
                    userId = userId,
                    displayId = displayId,
                )?.toRoutingImageData(userId)
            },
            purpose = ImageStorageGateway.Purpose.USER,
        )
    }

    get(LocalImageApiPath.adminImageV1ByDisplayId("{displayId}")) {
        val isAuthorized = call.requireAdminAuthorization(diContainer = diContainer)
        if (!isAuthorized) return@get

        call.respondImageByDisplayId(
            diContainer = diContainer,
            getImageData = { displayId ->
                diContainer.createAdminImageRepository().getImageDataByDisplayId(displayId)
                    ?.toRoutingImageData()
            },
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
    getImageData: (displayId: String) -> RoutingImageData?,
    purpose: ImageStorageGateway.Purpose,
) {
    val displayId = parameters["displayId"]
    if (displayId == null) {
        respondApiError(
            status = HttpStatusCode.BadRequest,
            message = "InvalidImageId",
        )
        return
    }

    val imageData = getImageData(displayId)
    if (imageData == null) {
        respondApiError(
            status = HttpStatusCode.NotFound,
            message = "NotFound",
        )
        return
    }

    val gateway = diContainer.createReadImageStorageGateway(imageData.storageType)

    when (gateway.storageType) {
        ImageStorageGateway.StorageType.LOCAL -> {
            val localGateway = gateway as? LocalImageStorageGateway
            if (localGateway == null) {
                respondApiError(
                    status = HttpStatusCode.InternalServerError,
                    message = "Invalid gateway type",
                )
                return
            }

            val inputStream: InputStream? = localGateway.openInputStream(imageData.relativePath)

            if (inputStream == null) {
                respondApiError(
                    status = HttpStatusCode.NotFound,
                    message = "NotFound",
                )
                return
            }

            val responseContentType = runCatching {
                ContentType.parse(imageData.contentType)
            }.getOrDefault(ContentType.Application.OctetStream)

            respondOutputStream(contentType = responseContentType) {
                inputStream.use { it.copyTo(this) }
            }
        }
        ImageStorageGateway.StorageType.S3 -> {
            val url = gateway.buildDisplayUrl(
                ImageStorageGateway.BuildUrlRequest(
                    domain = ServerEnv.domain ?: "",
                    displayId = displayId,
                    userId = imageData.userId,
                    relativePath = imageData.relativePath,
                    purpose = purpose,
                ),
            )
            respondRedirect(url = url, permanent = false)
        }
    }
}

private fun UserImageRepository.ImageData.toRoutingImageData(userId: net.matsudamper.money.element.UserId) = RoutingImageData(
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
    val userId: net.matsudamper.money.element.UserId,
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
