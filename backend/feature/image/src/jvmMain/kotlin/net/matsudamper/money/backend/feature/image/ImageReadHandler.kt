package net.matsudamper.money.backend.feature.image

import java.io.File

class ImageReadHandler {
    fun handle(request: Request): Result {
        if (!isValidImageHash(request.imageHash)) {
            return Result.BadRequest(message = "InvalidImageHash")
        }

        val file = resolveImageFile(
            storageDirectory = request.storageDirectory,
            relativePath = request.relativePath,
        ) ?: return Result.BadRequest(message = "InvalidImagePath")

        if (!file.exists() || !file.isFile) {
            return Result.NotFound
        }

        return Result.Success(file = file)
    }

    private fun resolveImageFile(
        storageDirectory: File,
        relativePath: String,
    ): File? {
        if (!isValidRelativePath(relativePath)) return null

        val root = runCatching { storageDirectory.canonicalFile }.getOrNull() ?: return null
        val file = runCatching { File(root, relativePath).canonicalFile }.getOrNull() ?: return null
        val rootPath = root.path
        val filePath = file.path
        val startsWithRoot =
            filePath == rootPath || filePath.startsWith("$rootPath${File.separator}")
        return if (startsWithRoot) file else null
    }

    private fun isValidImageHash(imageHash: String): Boolean {
        return IMAGE_HASH_REGEX.matches(imageHash)
    }

    private fun isValidRelativePath(relativePath: String): Boolean {
        if (relativePath.isBlank()) return false
        if (relativePath.contains("..")) return false
        if (relativePath.startsWith('/')) return false
        if (relativePath.startsWith('\\')) return false
        return true
    }

    data class Request(
        val imageHash: String,
        val relativePath: String,
        val storageDirectory: File,
    )

    sealed interface Result {
        data class Success(val file: File) : Result
        data class BadRequest(val message: String) : Result
        data object NotFound : Result
    }

    private companion object {
        private val IMAGE_HASH_REGEX = Regex("^[a-f0-9]{64}$")
    }
}
