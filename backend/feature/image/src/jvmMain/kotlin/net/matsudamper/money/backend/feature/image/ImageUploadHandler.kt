package net.matsudamper.money.backend.feature.image

import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import net.matsudamper.money.backend.app.interfaces.UserImageRepository
import net.matsudamper.money.element.UserId

class ImageUploadHandler {

    fun handle(request: Request): Result {
        val userId = request.userId ?: return Result.Unauthorized

        if (!request.storageDirectory.exists() && !request.storageDirectory.mkdirs()) {
            return Result.InternalServerError
        }

        val extension = resolveImageExtension(
            contentType = request.contentType,
        ) ?: return Result.UnsupportedMediaType

        val writeResult = writeImageFile(
            inputStream = request.inputStream,
            storageDirectory = request.storageDirectory,
            maxUploadBytes = request.maxUploadBytes,
        )
        return when (writeResult) {
            WriteImageFileResult.Empty -> Result.BadRequest(message = "EmptyFile")
            WriteImageFileResult.PayloadTooLarge -> Result.PayloadTooLarge
            is WriteImageFileResult.Success -> {
                val relativePath = createRelativePath(
                    imageHash = writeResult.imageHash,
                    extension = extension,
                )
                val destination = File(request.storageDirectory, relativePath)
                val destinationExisted = destination.exists()
                if (!moveTempFile(tempFile = writeResult.tempFile, destination = destination)) {
                    Result.InternalServerError
                } else {
                    val saveResult = request.userImageRepository.saveImage(
                        userId = userId,
                        imageHash = writeResult.imageHash,
                        relativePath = relativePath,
                    )
                    if (!saveResult) {
                        if (!destinationExisted) {
                            destination.delete()
                        }
                        return Result.InternalServerError
                    }

                    Result.Success(
                        imageHash = writeResult.imageHash,
                        relativePath = relativePath,
                    )
                }
            }

            WriteImageFileResult.SystemFailure -> Result.InternalServerError
        }
    }

    private fun writeImageFile(
        inputStream: InputStream,
        storageDirectory: File,
        maxUploadBytes: Long,
    ): WriteImageFileResult {
        val tempFile = runCatching {
            File.createTempFile("upload_", ".tmp", storageDirectory)
        }.getOrElse {
            return WriteImageFileResult.SystemFailure
        }
        var totalSize = 0L
        var isPayloadTooLarge = false
        val digest = MessageDigest.getInstance("SHA-256")

        val writeResult = runCatching {
            tempFile.outputStream().buffered().use { output ->
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
                    digest.update(buffer, 0, readSize)
                    output.write(buffer, 0, readSize)
                }
            }
        }
        if (writeResult.isFailure) {
            tempFile.delete()
            return WriteImageFileResult.SystemFailure
        }

        if (isPayloadTooLarge) {
            tempFile.delete()
            return WriteImageFileResult.PayloadTooLarge
        }

        if (totalSize <= 0L) {
            tempFile.delete()
            return WriteImageFileResult.Empty
        }

        return WriteImageFileResult.Success(
            tempFile = tempFile,
            imageHash = digest.digest().toHexString(),
        )
    }

    private fun moveTempFile(
        tempFile: File,
        destination: File,
    ): Boolean {
        if (destination.exists()) {
            tempFile.delete()
            return true
        }

        val parent = destination.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            tempFile.delete()
            return false
        }

        return runCatching {
            if (tempFile.renameTo(destination)) {
                true
            } else {
                tempFile.copyTo(destination, overwrite = false)
                tempFile.delete()
                true
            }
        }.getOrElse {
            destination.delete()
            tempFile.delete()
            false
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
        imageHash: String,
        extension: String,
    ): String {
        return "${imageHash.take(2)}/$imageHash.$extension"
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
            val imageHash: String,
            val relativePath: String,
        ) : Result

        data object Unauthorized : Result
        data class BadRequest(val message: String) : Result
        data object PayloadTooLarge : Result
        data object UnsupportedMediaType : Result
        data object InternalServerError : Result
    }

    private sealed interface WriteImageFileResult {
        data class Success(
            val tempFile: File,
            val imageHash: String,
        ) : WriteImageFileResult

        data object PayloadTooLarge : WriteImageFileResult
        data object Empty : WriteImageFileResult
        data object SystemFailure : WriteImageFileResult
    }
}

private fun ByteArray.toHexString(): String {
    return joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }
}
