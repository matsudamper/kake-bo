package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.UserId

public interface ImageResizerGateway {
    public fun createThumbnail(request: Request): Result

    public data class Request(
        val userId: UserId,
        val sourceRelativePath: String,
        val thumbnailRelativePath: String,
    )

    public sealed interface Result {
        public data object Success : Result
        public data object UnsupportedFormat : Result
        public data object TooLarge : Result
        public data class ConvertFailed(val message: String) : Result

        /** 変換サービスに到達できない、またはタイムアウト。クライアントは再試行してよい */
        public data class Unavailable(val cause: Throwable) : Result
    }
}
