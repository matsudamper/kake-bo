package net.matsudamper.money.backend.graphql.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.matsudamper.money.backend.app.interfaces.AdminImageRepository
import net.matsudamper.money.backend.app.interfaces.ImageResizerGateway
import net.matsudamper.money.backend.feature.image.ThumbnailRelativePath
import net.matsudamper.money.element.ImageId

/**
 * サムネイルが無い S3 の画像を 1 件ずつ生成する。
 * 購読側がキャンセルすると次の画像に進む前に止まる（処理中の 1 件は完了まで待つ）。
 */
class AdminThumbnailBackfillUseCase(
    private val adminImageRepository: AdminImageRepository,
    private val imageResizerGateway: ImageResizerGateway,
) {
    sealed interface Event {
        val imageId: ImageId

        data class Success(override val imageId: ImageId) : Event
        data class Failed(override val imageId: ImageId, val message: String) : Event
    }

    fun run(): Flow<Event> = flow {
        var afterImageId: ImageId? = null
        while (true) {
            val images = adminImageRepository.getS3ImagesWithoutThumbnail(
                size = BATCH_SIZE,
                afterImageId = afterImageId,
            )
            if (images.isEmpty()) return@flow

            for (image in images) {
                currentCoroutineContext().ensureActive()
                emit(process(image))
                afterImageId = image.imageId
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun process(image: AdminImageRepository.ImageWithoutThumbnail): Event {
        val thumbnailRelativePath = ThumbnailRelativePath.of(image.relativePath)
        val result = imageResizerGateway.createThumbnail(
            ImageResizerGateway.Request(
                userId = image.userId,
                sourceRelativePath = image.relativePath,
                thumbnailRelativePath = thumbnailRelativePath,
            ),
        )
        return when (result) {
            ImageResizerGateway.Result.Success -> {
                adminImageRepository.updateThumbnailPath(
                    imageId = image.imageId,
                    thumbnailRelativePath = thumbnailRelativePath,
                )
                Event.Success(image.imageId)
            }

            ImageResizerGateway.Result.UnsupportedFormat -> Event.Failed(image.imageId, "unsupported format")
            ImageResizerGateway.Result.TooLarge -> Event.Failed(image.imageId, "too large")
            is ImageResizerGateway.Result.ConvertFailed -> Event.Failed(image.imageId, "convert failed: ${result.message}")
            is ImageResizerGateway.Result.Unavailable -> Event.Failed(image.imageId, "resizer unavailable: ${result.cause.message}")
        }
    }

    private companion object {
        const val BATCH_SIZE = 50
    }
}
