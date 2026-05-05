package net.matsudamper.money.backend.feature.image

import java.io.InputStream
import java.time.YearMonth
import java.util.UUID
import net.matsudamper.money.backend.app.interfaces.ImageStorageGateway
import net.matsudamper.money.backend.app.interfaces.UserImageRepository
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.UserId

class ImageUploadHandler {

    fun handle(request: Request): Result {
        val userId = request.userId ?: return Result.Unauthorized

        val unsupportedMediaType = Result.UnsupportedMediaType(mediaType = request.contentType ?: "null")
        val extension = resolveImageExtension(
            contentType = request.contentType,
        ) ?: return unsupportedMediaType
        val contentType = request.contentType ?: return unsupportedMediaType

        val displayUUID = UUID.randomUUID().toString()
        val relativePath = createRelativePath(
            displayId = displayUUID,
            extension = extension,
        )

        val storageType = when (request.imageStorageGateway.storageType) {
            ImageStorageGateway.StorageType.LOCAL -> UserImageRepository.StorageType.LOCAL
            ImageStorageGateway.StorageType.S3 -> UserImageRepository.StorageType.S3
        }

        val imageId = request.userImageRepository.saveImage(
            userId = userId,
            displayId = displayUUID,
            relativePath = relativePath,
            contentType = contentType,
            storageType = storageType,
        ) ?: return Result.InternalServerError(IllegalStateException("Failed to reserve image ID. UUID: $displayUUID"))

        val writeResult = request.imageStorageGateway.put(
            ImageStorageGateway.PutRequest(
                userId = userId,
                relativePath = relativePath,
                contentType = contentType,
                contentLength = request.contentLength,
                maxBytes = request.maxUploadBytes,
                inputStream = request.inputStream,
            ),
        )

        return when (writeResult) {
            ImageStorageGateway.PutResult.Empty -> {
                request.userImageRepository.deleteReserveImage(userId = userId, imageId = imageId)
                Result.BadRequest(message = "EmptyFile")
            }

            ImageStorageGateway.PutResult.PayloadTooLarge -> {
                request.userImageRepository.deleteReserveImage(userId = userId, imageId = imageId)
                Result.PayloadTooLarge
            }

            is ImageStorageGateway.PutResult.Failure -> {
                request.userImageRepository.deleteReserveImage(userId = userId, imageId = imageId)
                Result.InternalServerError(writeResult.cause)
            }

            is ImageStorageGateway.PutResult.Success -> {
                request.userImageRepository.markImageAsUploaded(userId = userId, imageId = imageId)
                Result.Success(
                    imageId = imageId,
                    displayId = displayUUID,
                )
            }
        }
    }

    private fun resolveImageExtension(
        contentType: String?,
    ): String? {
        return when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "image/bmp" -> "bmp"
            "image/heic" -> "heic"
            "image/heif" -> "heif"
            else -> null
        }
    }

    private fun createRelativePath(
        displayId: String,
        extension: String,
    ): String {
        val yearMonth = YearMonth.now().toString()
        return "$yearMonth/$displayId.$extension"
    }

    data class Request(
        val userId: UserId?,
        val userImageRepository: UserImageRepository,
        val imageStorageGateway: ImageStorageGateway,
        val maxUploadBytes: Long,
        val contentLength: Long?,
        val contentType: String?,
        val inputStream: InputStream,
    )

    sealed interface Result {
        data class Success(
            val imageId: ImageId,
            val displayId: String,
        ) : Result

        data object Unauthorized : Result
        data class BadRequest(val message: String) : Result
        data object PayloadTooLarge : Result
        data class UnsupportedMediaType(val mediaType: String) : Result
        data class InternalServerError(val e: Throwable) : Result
    }
}
