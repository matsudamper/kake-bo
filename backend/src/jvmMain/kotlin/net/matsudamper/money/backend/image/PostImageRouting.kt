package net.matsudamper.money.backend.image

import kotlinx.serialization.json.Json
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.utils.io.jvm.javaio.toInputStream
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.backend.feature.image.ImageApiPath
import net.matsudamper.money.backend.feature.image.ImageUploadHandler
import net.matsudamper.money.backend.feature.session.KtorCookieManager
import net.matsudamper.money.backend.feature.session.UserSessionManagerImpl
import net.matsudamper.money.image.ImageUploadApiPath
import net.matsudamper.money.image.ImageUploadImageResponse
import net.matsudamper.money.image.ImageUploadImageResponse.Success

internal fun Route.postImage(
    diContainer: DiContainer,
    config: ImageUploadConfig,
    imageUploadHandler: ImageUploadHandler = ImageUploadHandler(),
) {
    post(ImageUploadApiPath.uploadV1) {
        val userId = UserSessionManagerImpl(
            cookieManager = KtorCookieManager(call = call),
            userSessionRepository = diContainer.createUserSessionRepository(),
        ).verifyUserSession()
        if (userId == null) {
            call.respondApiError(
                status = HttpStatusCode.Unauthorized,
                message = "Unauthorized",
            )
            return@post
        }

        val multipart = call.receiveMultipart()

        var result: ImageUploadHandler.Result? = null
        while (true) {
            val part = multipart.readPart() ?: break
            if (part !is PartData.FileItem) {
                part.dispose()
                continue
            }
            if (result != null) {
                part.dispose()
                while (true) {
                    multipart.readPart()?.dispose() ?: break
                }
                call.respondApiError(
                    status = HttpStatusCode.BadRequest,
                    message = HttpStatusCode.BadRequest.description,
                )
                return@post
            }
            result = part.provider().toInputStream().use { inputStream ->
                imageUploadHandler.handle(
                    request = ImageUploadHandler.Request(
                        userId = userId,
                        userImageRepository = diContainer.createUserImageRepository(),
                        storageDirectory = config.storageDirectory,
                        maxUploadBytes = config.maxUploadBytes,
                        contentType = part.contentType?.withoutParameters()?.toString(),
                        inputStream = inputStream,
                    ),
                )
            }
            part.dispose()
        }

        when (val uploadResult = result) {
            null -> {
                call.respondApiError(
                    status = HttpStatusCode.BadRequest,
                    message = "NoFile",
                )
            }

            is ImageUploadHandler.Result.BadRequest -> {
                call.respondApiError(
                    status = HttpStatusCode.BadRequest,
                    message = uploadResult.message,
                )
            }

            ImageUploadHandler.Result.Unauthorized -> {
                call.respondApiError(
                    status = HttpStatusCode.Unauthorized,
                    message = "Unauthorized",
                )
            }

            is ImageUploadHandler.Result.InternalServerError -> {
                TraceLogger.impl().noticeThrowable(uploadResult.e, true)
                call.respondApiError(
                    status = HttpStatusCode.InternalServerError,
                    message = "InternalServerError",
                )
            }

            ImageUploadHandler.Result.PayloadTooLarge -> {
                call.respondApiError(
                    status = HttpStatusCode.PayloadTooLarge,
                    message = "FileTooLarge",
                )
            }

            is ImageUploadHandler.Result.Success -> {
                val domain = ServerEnv.domain
                    ?: throw IllegalStateException("DOMAIN is not configured")
                call.respondText(
                    status = HttpStatusCode.Created,
                    contentType = ContentType.Application.Json,
                    text = Json.encodeToString(
                        ImageUploadImageResponse(
                            success = Success(
                                imageId = uploadResult.imageId,
                                url = ImageApiPath.imageV1AbsoluteByDisplayId(
                                    domain = domain,
                                    displayId = uploadResult.displayId,
                                ),
                            ),
                        ),
                    ),
                )
            }

            ImageUploadHandler.Result.UnsupportedMediaType -> {
                call.respondApiError(
                    status = HttpStatusCode.UnsupportedMediaType,
                    message = "UnsupportedMediaType",
                )
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
