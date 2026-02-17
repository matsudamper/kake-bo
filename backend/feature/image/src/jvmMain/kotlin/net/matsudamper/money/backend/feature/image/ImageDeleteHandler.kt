package net.matsudamper.money.backend.feature.image

import java.io.File
import net.matsudamper.money.backend.app.interfaces.UserImageRepository
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.UserId

class ImageDeleteHandler {

    fun handle(request: Request): Result {
        val userId = request.userId ?: return Result.Unauthorized

        return runCatching {
            request.userImageRepository.deleteImageUsageRelation(
                userId = userId,
                moneyUsageId = request.moneyUsageId,
                imageId = request.imageId,
            )

            val remainingRelationCount = request.userImageRepository.countImageUsageRelations(
                userId = userId,
                imageId = request.imageId,
            )

            if (remainingRelationCount > 0) {
                return Result.Success
            }

            val relativePath = request.userImageRepository.getRelativePath(
                userId = userId,
                imageId = request.imageId,
            ) ?: return Result.NotFound

            val deleted = File(request.storageDirectory, relativePath).delete()
            if (!deleted) {
                throw IllegalStateException("ファイルの削除に失敗しました: $relativePath")
            }

            request.userImageRepository.deleteImage(
                userId = userId,
                imageId = request.imageId,
            )

            Result.Success
        }.fold(
            onSuccess = { it },
            onFailure = { Result.InternalServerError(it) },
        )
    }

    data class Request(
        val userId: UserId?,
        val imageId: ImageId,
        val moneyUsageId: MoneyUsageId,
        val userImageRepository: UserImageRepository,
        val storageDirectory: File,
    )

    sealed interface Result {
        data object Success : Result
        data object Unauthorized : Result
        data object NotFound : Result
        data class InternalServerError(val e: Throwable) : Result
    }
}
