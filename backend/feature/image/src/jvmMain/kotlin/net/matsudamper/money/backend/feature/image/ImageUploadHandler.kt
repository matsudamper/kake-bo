package net.matsudamper.money.backend.feature.image

import java.io.InputStream
import java.time.YearMonth
import java.util.UUID
import net.matsudamper.money.backend.app.interfaces.ImageResizerGateway
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
        val yearMonth = YearMonth.now().toString()
        val relativePath = createRelativePath(
            yearMonth = yearMonth,
            displayId = displayUUID,
            extension = extension,
        )
        val thumbnailRelativePath = ThumbnailRelativePath.of(relativePath)

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
                completeWithThumbnail(
                    request = request,
                    userId = userId,
                    imageId = imageId,
                    displayId = displayUUID,
                    relativePath = relativePath,
                    thumbnailRelativePath = thumbnailRelativePath,
                )
            }
        }
    }

    /**
     * サムネイル生成の成功をアップロード成功の条件にする。
     * 失敗したら元画像と予約行を消し、クライアントにはアップロード失敗として返す。
     */
    private fun completeWithThumbnail(
        request: Request,
        userId: UserId,
        imageId: ImageId,
        displayId: String,
        relativePath: String,
        thumbnailRelativePath: String,
    ): Result {
        val imageResizerGateway = request.imageResizerGateway
        val thumbnailResult = imageResizerGateway?.createThumbnail(
            ImageResizerGateway.Request(
                userId = userId,
                sourceRelativePath = relativePath,
                thumbnailRelativePath = thumbnailRelativePath,
            ),
        )
        val storedThumbnailRelativePath = when (thumbnailResult) {
            null -> null
            ImageResizerGateway.Result.Success -> thumbnailRelativePath
            ImageResizerGateway.Result.UnsupportedFormat,
            ImageResizerGateway.Result.TooLarge,
            is ImageResizerGateway.Result.ConvertFailed,
            is ImageResizerGateway.Result.Unavailable,
            -> {
                rollbackUpload(
                    request = request,
                    userId = userId,
                    imageId = imageId,
                    relativePaths = listOf(relativePath, thumbnailRelativePath),
                )
                return Result.ThumbnailFailed(thumbnailResult)
            }
        }

        request.userImageRepository.markImageAsUploaded(
            userId = userId,
            imageId = imageId,
            thumbnailRelativePath = storedThumbnailRelativePath,
        )
        return Result.Success(
            imageId = imageId,
            displayId = displayId,
        )
    }

    private fun rollbackUpload(
        request: Request,
        userId: UserId,
        imageId: ImageId,
        relativePaths: List<String>,
    ) {
        for (relativePath in relativePaths) {
            request.imageStorageGateway.delete(
                ImageStorageGateway.DeleteRequest(
                    userId = userId,
                    relativePath = relativePath,
                ),
            )
        }
        request.userImageRepository.deleteReserveImage(userId = userId, imageId = imageId)
    }

    private fun resolveImageExtension(
        contentType: String?,
    ): String? {
        return when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "image/heic" -> "heic"
            "image/heif" -> "heif"
            else -> null
        }
    }

    private fun createRelativePath(
        yearMonth: String,
        displayId: String,
        extension: String,
    ): String {
        return "$yearMonth/$displayId.$extension"
    }

    data class Request(
        val userId: UserId?,
        val userImageRepository: UserImageRepository,
        val imageStorageGateway: ImageStorageGateway,
        val imageResizerGateway: ImageResizerGateway?,
        val maxUploadBytes: Long,
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
        data class ThumbnailFailed(val cause: ImageResizerGateway.Result) : Result
        data class InternalServerError(val e: Throwable) : Result
    }
}
