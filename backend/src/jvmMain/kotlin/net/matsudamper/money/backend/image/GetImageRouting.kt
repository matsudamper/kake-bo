package net.matsudamper.money.backend.image

import kotlinx.serialization.json.Json
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import net.matsudamper.money.backend.app.interfaces.UserImageRepository
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.backend.feature.image.ImageReadHandler
import net.matsudamper.money.backend.feature.session.KtorCookieManager
import net.matsudamper.money.backend.feature.session.UserSessionManagerImpl
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.image.ImageUploadImageResponse

internal fun Route.getImage(
    diContainer: DiContainer,
    userImageRepository: UserImageRepository,
    imageUploadConfig: ImageUploadConfig,
    imageReadHandler: ImageReadHandler = ImageReadHandler(),
) {
    get("/api/image/v1/{hash}") {
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

        val imageHash = call.parameters["hash"]
        if (imageHash == null) {
            call.respondApiError(
                status = HttpStatusCode.BadRequest,
                message = "InvalidImageHash",
            )
            return@get
        }
        if (!IMAGE_HASH_REGEX.matches(imageHash)) {
            call.respondApiError(
                status = HttpStatusCode.BadRequest,
                message = "InvalidImageHash",
            )
            return@get
        }

        val imageId = ImageId(imageHash)
        val relativePath = userImageRepository.getRelativePath(
            userId = userId,
            imageId = imageId,
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
                    imageHash = imageHash,
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

private val IMAGE_HASH_REGEX = Regex("^[a-f0-9]{64}$")
