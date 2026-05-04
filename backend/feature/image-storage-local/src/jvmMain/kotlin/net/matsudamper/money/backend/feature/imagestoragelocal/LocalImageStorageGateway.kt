package net.matsudamper.money.backend.feature.imagestoragelocal

import java.io.File
import java.io.InputStream
import net.matsudamper.money.backend.app.interfaces.ImageStorageGateway

public class LocalImageStorageGateway(
    private val storageDirectory: File,
) : ImageStorageGateway {
    override val storageType: ImageStorageGateway.StorageType = ImageStorageGateway.StorageType.LOCAL

    override fun put(request: ImageStorageGateway.PutRequest): ImageStorageGateway.PutResult {
        val destination = File(storageDirectory, request.relativePath)

        val parent = destination.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return ImageStorageGateway.PutResult.Failure(IllegalStateException("Failed to create parent directory for: ${destination.absolutePath}"))
        }

        var totalSize = 0L
        var isPayloadTooLarge = false

        val writeResult = runCatching {
            destination.outputStream().buffered().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val readSize = request.inputStream.read(buffer)
                    if (readSize < 0) break
                    if (readSize == 0) continue

                    totalSize += readSize.toLong()
                    if (totalSize > request.maxBytes) {
                        isPayloadTooLarge = true
                        break
                    }
                    output.write(buffer, 0, readSize)
                }
            }
        }
        if (writeResult.isFailure) {
            destination.delete()
            return ImageStorageGateway.PutResult.Failure(writeResult.exceptionOrNull() ?: IllegalStateException("Failed to write image file to destination: ${destination.absolutePath}"))
        }

        if (isPayloadTooLarge) {
            destination.delete()
            return ImageStorageGateway.PutResult.PayloadTooLarge
        }

        if (totalSize <= 0L) {
            destination.delete()
            return ImageStorageGateway.PutResult.Empty
        }

        return ImageStorageGateway.PutResult.Success(relativePath = request.relativePath)
    }

    override fun buildDisplayUrl(request: ImageStorageGateway.BuildUrlRequest): String {
        return when (request.purpose) {
            ImageStorageGateway.Purpose.USER -> "https://${request.domain}/api/image/v1/${request.displayId}"
            ImageStorageGateway.Purpose.ADMIN -> "https://${request.domain}/api/admin/image/v1/${request.displayId}"
        }
    }

    public fun openInputStream(relativePath: String): InputStream? {
        val file = File(storageDirectory, relativePath)
        return if (file.exists()) {
            file.inputStream()
        } else {
            null
        }
    }
}
