package net.matsudamper.money.backend.feature.imagestoragelocal

import java.io.File
import java.io.InputStream
import java.nio.file.Path
import net.matsudamper.money.backend.app.interfaces.ImageStorageGateway

class LocalImageStorageGateway(
    private val storageDirectory: File,
) : ImageStorageGateway {
    override val storageType: ImageStorageGateway.StorageType = ImageStorageGateway.StorageType.LOCAL

    override fun put(request: ImageStorageGateway.PutRequest): ImageStorageGateway.PutResult {
        val destination = resolveSecurePath(request.relativePath)

        val parent = destination.toFile().parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return ImageStorageGateway.PutResult.Failure(IllegalStateException("Failed to create parent directory for: $destination"))
        }

        var totalSize = 0L
        var isPayloadTooLarge = false

        val writeResult = runCatching {
            destination.toFile().outputStream().buffered().use { output ->
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
            destination.toFile().delete()
            return ImageStorageGateway.PutResult.Failure(writeResult.exceptionOrNull() ?: IllegalStateException("Failed to write image file to destination: ${destination.toFile().absolutePath}"))
        }

        if (isPayloadTooLarge) {
            destination.toFile().delete()
            return ImageStorageGateway.PutResult.PayloadTooLarge
        }

        if (totalSize <= 0L) {
            destination.toFile().delete()
            return ImageStorageGateway.PutResult.Empty
        }

        return ImageStorageGateway.PutResult.Success(relativePath = request.relativePath)
    }

    override fun delete(request: ImageStorageGateway.DeleteRequest): ImageStorageGateway.DeleteResult {
        val path = resolveSecurePath(request.relativePath)
        val file = path.toFile()
        if (!file.exists()) return ImageStorageGateway.DeleteResult.Success
        return if (file.delete()) {
            ImageStorageGateway.DeleteResult.Success
        } else {
            ImageStorageGateway.DeleteResult.Failure(IllegalStateException("ファイルの削除に失敗しました: $path"))
        }
    }

    override fun buildDisplayUrl(request: ImageStorageGateway.BuildUrlRequest): String {
        return when (request.purpose) {
            ImageStorageGateway.Purpose.USER -> "https://${request.domain}/api/image/v1/${request.displayId}"
            ImageStorageGateway.Purpose.ADMIN -> "https://${request.domain}/api/admin/image/v1/${request.displayId}"
        }
    }

    fun openInputStream(relativePath: String): InputStream? {
        val file = resolveSecurePath(relativePath)
        return file.toFile().takeIf { it.exists() }?.inputStream()
    }

    /**
     * パストラバーサル検証: relativePath を root 配下に解決し、root 外なら null を返す
     */
    private fun resolveSecurePath(relativePath: String): Path {
        val root = storageDirectory.toPath().toAbsolutePath().normalize()
        val resolved = root.resolve(relativePath).normalize()
        return if (resolved.startsWith(root)) {
            resolved
        } else {
            throw SecurityException("Path traversal detected: $relativePath")
        }
    }
}
