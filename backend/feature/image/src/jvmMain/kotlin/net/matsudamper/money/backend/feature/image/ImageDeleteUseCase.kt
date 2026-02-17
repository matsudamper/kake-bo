package net.matsudamper.money.backend.feature.image

import java.io.File
import net.matsudamper.money.backend.app.interfaces.UserImageRepository
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.UserId

class ImageDeleteUseCase {

    fun delete(
        userId: UserId,
        imageId: ImageId,
        moneyUsageId: MoneyUsageId,
        userImageRepository: UserImageRepository,
        storageDirectory: File,
    ): Result {
        return runCatching {
            userImageRepository.deleteImageUsageRelation(
                userId = userId,
                moneyUsageId = moneyUsageId,
                imageId = imageId,
            )

            val remainingRelationCount = userImageRepository.countImageUsageRelations(
                userId = userId,
                imageId = imageId,
            )

            if (remainingRelationCount > 0) {
                return Result.Success
            }

            val relativePath = userImageRepository.getRelativePath(
                userId = userId,
                imageId = imageId,
            ) ?: return Result.NotFound

            val deleted = File(storageDirectory, relativePath).delete()
            if (!deleted) {
                throw IllegalStateException("ファイルの削除に失敗しました: $relativePath")
            }

            userImageRepository.deleteImage(
                userId = userId,
                imageId = imageId,
            )

            Result.Success
        }.fold(
            onSuccess = { it },
            onFailure = { Result.InternalServerError(it) },
        )
    }

    sealed interface Result {
        data object Success : Result
        data object NotFound : Result
        data class InternalServerError(val e: Throwable) : Result
    }
}
