package net.matsudamper.money.backend.feature.image

import java.io.File
import java.io.InputStream
import java.time.YearMonth
import java.util.UUID
import net.matsudamper.money.backend.app.interfaces.UserImageRepository
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.UserId

class ImageUploadHandler {

    fun handle(request: Request): Result {
        val userId = request.userId ?: return Result.Unauthorized

        if (!request.storageDirectory.exists() && !request.storageDirectory.mkdirs()) {
            return Result.InternalServerError(IllegalStateException("Failed to create storage directory: ${request.storageDirectory.absolutePath}"))
        }

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
        val imageId = request.userImageRepository.saveImage(
            userId = userId,
            displayId = displayUUID,
            relativePath = relativePath,
            contentType = contentType,
        ) ?: return Result.InternalServerError(IllegalStateException("Failed to reserve image ID. UUID: $displayUUID"))
        val destination = File(request.storageDirectory, relativePath)
        if (destination.exists()) {
            request.userImageRepository.deleteImage(userId = userId, imageId = imageId)
            return Result.InternalServerError(IllegalStateException("File already exists at destination: ${destination.absolutePath}"))
        }

        val writeResult = writeImageFile(
            inputStream = request.inputStream,
            destination = destination,
            maxUploadBytes = request.maxUploadBytes,
        )
        return when (writeResult) {
            WriteImageFileResult.Empty -> {
                request.userImageRepository.deleteImage(userId = userId, imageId = imageId)
                Result.BadRequest(message = "EmptyFile")
            }

            WriteImageFileResult.PayloadTooLarge -> {
                request.userImageRepository.deleteImage(userId = userId, imageId = imageId)
                Result.PayloadTooLarge
            }

            WriteImageFileResult.SystemFailure -> {
                request.userImageRepository.deleteImage(userId = userId, imageId = imageId)
                Result.InternalServerError(IllegalStateException("Failed to write image file to destination: ${destination.absolutePath}"))
            }

            WriteImageFileResult.Success -> {
                request.userImageRepository.markImageAsUploaded(userId = userId, imageId = imageId)
                Result.Success(
                    imageId = imageId,
                    displayId = displayUUID,
                    relativePath = relativePath,
                )
            }
        }
    }

    private fun writeImageFile(
        inputStream: InputStream,
        destination: File,
        maxUploadBytes: Long,
    ): WriteImageFileResult {
        val parent = destination.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return WriteImageFileResult.SystemFailure
        }

        var totalSize = 0L
        var isPayloadTooLarge = false

        val writeResult = runCatching {
            destination.outputStream().buffered().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val readSize = inputStream.read(buffer)
                    if (readSize < 0) break
                    if (readSize == 0) continue

                    totalSize += readSize.toLong()
                    if (totalSize > maxUploadBytes) {
                        isPayloadTooLarge = true
                        break
                    }
                    output.write(buffer, 0, readSize)
                }
            }
        }
        if (writeResult.isFailure) {
            destination.delete()
            return WriteImageFileResult.SystemFailure
        }

        if (isPayloadTooLarge) {
            destination.delete()
            return WriteImageFileResult.PayloadTooLarge
        }

        if (totalSize <= 0L) {
            destination.delete()
            return WriteImageFileResult.Empty
        }

        return WriteImageFileResult.Success
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
        val storageDirectory: File,
        val maxUploadBytes: Long,
        val contentType: String?,
        val inputStream: InputStream,
    )

    sealed interface Result {
        data class Success(
            val imageId: ImageId,
            val displayId: String,
            val relativePath: String,
        ) : Result

        data object Unauthorized : Result
        data class BadRequest(val message: String) : Result
        data object PayloadTooLarge : Result
        data class UnsupportedMediaType(val mediaType: String) : Result
        data class InternalServerError(val e: Throwable) : Result
    }

    private sealed interface WriteImageFileResult {
        data object Success : WriteImageFileResult
        data object PayloadTooLarge : WriteImageFileResult
        data object Empty : WriteImageFileResult
        data object SystemFailure : WriteImageFileResult
    }
}
