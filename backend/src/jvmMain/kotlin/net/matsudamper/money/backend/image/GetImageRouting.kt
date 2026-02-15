package net.matsudamper.money.backend.image

import kotlinx.serialization.json.Json
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
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
        val userId = UserSessionManagerImpl(
            cookieManager = KtorCookieManager(call = call),
            userSessionRepository = diContainer.createUserSessionRepository(),
        ).verifyUserSession()
        if (userId == null) {
            call.respondApiError(
                status = HttpStatusCode.Unauthorized,
                message = "Unauthorized",
            )
            return@get
        }

        val displayId = call.parameters["displayId"]
        if (displayId == null) {
            call.respondApiError(
                status = HttpStatusCode.BadRequest,
                message = "InvalidImageId",
            )
            return@get
        }
        if (!DISPLAY_ID_REGEX.matches(displayId)) {
            call.respondApiError(
                status = HttpStatusCode.BadRequest,
                message = "InvalidImageId",
            )
            return@get
        }

        val relativePath = diContainer.createUserImageRepository().getRelativePathByDisplayId(
            userId = userId,
            displayId = displayId,
        )
        if (relativePath == null) {
            call.respondApiError(
                status = HttpStatusCode.NotFound,
                message = "NotFound",
            )
            return@get
        }

        when (
            val result = imageReadHandler.handle(
                request = ImageReadHandler.Request(
                    displayId = displayId,
                    relativePath = relativePath,
                    storageDirectory = imageUploadConfig.storageDirectory,
                ),
            )
        ) {
            is ImageReadHandler.Result.BadRequest -> {
                call.respondApiError(
                    status = HttpStatusCode.BadRequest,
                    message = result.message,
                )
            }

            ImageReadHandler.Result.NotFound -> {
                call.respondApiError(
                    status = HttpStatusCode.NotFound,
                    message = "NotFound",
                )
            }

            is ImageReadHandler.Result.Success -> {
                call.respondFile(result.file)
            }
        }
    }
}

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

private val DISPLAY_ID_REGEX =
    Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
