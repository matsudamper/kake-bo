package net.matsudamper.money.backend.app.interfaces

import java.io.InputStream
import net.matsudamper.money.element.UserId

public interface ImageStorageGateway {
    public enum class StorageType { LOCAL, S3 }
    public val storageType: StorageType

    public fun put(request: PutRequest): PutResult

    public fun delete(request: DeleteRequest): DeleteResult

    public fun buildDisplayUrl(request: BuildUrlRequest): String

    public data class PutRequest(
        val userId: UserId,
        val relativePath: String,
        val contentType: String,
        val maxBytes: Long,
        val inputStream: InputStream,
    )

    public sealed interface PutResult {
        public data class Success(val relativePath: String) : PutResult
        public data object PayloadTooLarge : PutResult
        public data object Empty : PutResult
        public data class Failure(val cause: Throwable) : PutResult
    }

    public data class DeleteRequest(
        val userId: UserId,
        val relativePath: String,
    )

    public sealed interface DeleteResult {
        public data object Success : DeleteResult
        public data class Failure(val cause: Throwable) : DeleteResult
    }

    public data class BuildUrlRequest(
        val domain: String,
        val displayId: String,
        val userId: UserId,
        val relativePath: String,
        val purpose: Purpose,
    )

    public enum class Purpose { USER, ADMIN }
}
