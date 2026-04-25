package net.matsudamper.money.backend.image

import kotlinx.serialization.json.Json
import io.ktor.client.content.LocalFileContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import net.matsudamper.money.backend.app.interfaces.AdminImageRepository
import net.matsudamper.money.backend.app.interfaces.UserImageRepository
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.backend.feature.image.ImageApiPath
import net.matsudamper.money.backend.feature.image.ImageReadHandler
import net.matsudamper.money.backend.feature.session.KtorCookieManager
import net.matsudamper.money.backend.feature.session.UserSessionManagerImpl
import net.matsudamper.money.image.ImageUploadImageResponse

internal fun Route.getImage(
    diContainer: DiContainer,
    imageUploadConfig: ImageUploadConfig,
    imageReadHandler: ImageReadHandler = ImageReadHandler(),
) {
    get(ImageApiPath.imageV1ByDisplayId("{displayId}")) {
        val userId = call.requireUserId(diContainer = diContainer) ?: return@get

        call.respondImageByDisplayId(
            imageUploadConfig = imageUploadConfig,
            imageReadHandler = imageReadHandler,
        ) { displayId ->
            diContainer.createUserImageRepository().getImageDataByDisplayId(
                userId = userId,
                displayId = displayId,
            )?.toRoutingImageData()
        }
    }

    get(ImageApiPath.adminImageV1ByDisplayId("{displayId}")) {
        val isAuthorized = call.requireAdminAuthorization(diContainer = diContainer)
        if (!isAuthorized) return@get

        call.respondImageByDisplayId(
            imageUploadConfig = imageUploadConfig,
            imageReadHandler = imageReadHandler,
        ) { displayId ->
            diContainer.createAdminImageRepository().getImageDataByDisplayId(
                displayId = displayId,
            )?.toRoutingImageData()
        }
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
    imageUploadConfig: ImageUploadConfig,
    imageReadHandler: ImageReadHandler,
    getImageData: (displayId: String) -> RoutingImageData?,
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

    when (
        val result = imageReadHandler.handle(
            request = ImageReadHandler.Request(
                displayId = displayId,
                relativePath = imageData.relativePath,
                storageDirectory = imageUploadConfig.storageDirectory,
            ),
        )
    ) {
        is ImageReadHandler.Result.BadRequest -> {
            respondApiError(
                status = HttpStatusCode.BadRequest,
                message = result.message,
            )
        }

        ImageReadHandler.Result.NotFound -> {
            respondApiError(
                status = HttpStatusCode.NotFound,
                message = "NotFound",
            )
        }

        is ImageReadHandler.Result.Success -> {
            val responseContentType = runCatching {
                ContentType.parse(imageData.contentType)
            }.getOrDefault(ContentType.Application.OctetStream)

            respond(
                LocalFileContent(
                    file = result.file,
                    contentType = responseContentType,
                ),
            )
        }
    }
}

private fun UserImageRepository.ImageData.toRoutingImageData() = RoutingImageData(
    relativePath = relativePath,
    contentType = contentType,
)

private fun AdminImageRepository.ImageData.toRoutingImageData() = RoutingImageData(
    relativePath = relativePath,
    contentType = contentType,
)

private data class RoutingImageData(
    val relativePath: String,
    val contentType: String,
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
