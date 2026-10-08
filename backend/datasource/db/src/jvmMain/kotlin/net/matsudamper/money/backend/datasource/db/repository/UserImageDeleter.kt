package net.matsudamper.money.backend.datasource.db.repository

import java.io.IOException
import net.matsudamper.money.backend.app.interfaces.ImageStorageGateway
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.backend.datasource.db.element.DbStorageType
import net.matsudamper.money.db.schema.tables.JMoneyUsageImagesRelation
import net.matsudamper.money.db.schema.tables.JUserImages
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.UserId
import org.jooq.DSLContext

internal class UserImageDeleter(
    private val localImageStorageGateway: ImageStorageGateway,
    private val s3ImageStorageGateway: ImageStorageGateway?,
) {
    /**
     * どの利用にも紐付いていない場合だけ画像レコードを削除する。
     * ファイルはトランザクションのコミット後に[deleteFile]で削除する
     * @return 削除したレコードのファイル情報。紐付きが残っていて削除しなかった場合はnull
     */
    fun deleteRecordIfUnlinked(
        context: DSLContext,
        userId: UserId,
        imageId: ImageId,
    ): DeletedImage? {
        val usageImagesRelation = JMoneyUsageImagesRelation.MONEY_USAGE_IMAGES_RELATION
        val linkedCount = context
            .selectCount()
            .from(usageImagesRelation)
            .where(
                usageImagesRelation.USER_ID.eq(userId.value)
                    .and(usageImagesRelation.USER_IMAGE_ID.eq(imageId.value)),
            )
            .fetchOne(0, Int::class.java) ?: 0
        if (linkedCount > 0) return null

        val userImages = JUserImages.USER_IMAGES
        val record = context
            .select(userImages.IMAGE_PATH, userImages.STORAGE_TYPE)
            .from(userImages)
            .where(
                userImages.USER_ID.eq(userId.value)
                    .and(userImages.USER_IMAGE_ID.eq(imageId.value)),
            )
            .fetchOne()
            ?: throw IllegalStateException("ユーザー画像が見つかりませんでした: userId=${userId.value}, imageId=${imageId.value}")

        val path = record.get(userImages.IMAGE_PATH)
            ?: throw IllegalStateException("画像パスが見つかりませんでした: imageId=${imageId.value}")
        val storageType = record.get(userImages.STORAGE_TYPE)

        context
            .deleteFrom(userImages)
            .where(
                userImages.USER_ID.eq(userId.value)
                    .and(userImages.USER_IMAGE_ID.eq(imageId.value)),
            )
            .limit(1)
            .execute()

        return DeletedImage(
            userId = userId,
            path = path,
            storageType = storageType,
        )
    }

    /**
     * 失敗してもDBは既にコミット済みで参照されないファイルが残るだけなので、例外にせず記録する
     */
    fun deleteFile(deletedImage: DeletedImage) {
        runCatching {
            val gateway = when (deletedImage.storageType) {
                DbStorageType.LOCAL.dbValue -> localImageStorageGateway
                DbStorageType.S3.dbValue -> s3ImageStorageGateway
                    ?: throw IllegalStateException("S3ストレージゲートウェイが設定されていません")

                else -> throw IllegalStateException("Unknown storage_type: ${deletedImage.storageType}")
            }
            val result = gateway.delete(
                ImageStorageGateway.DeleteRequest(
                    userId = deletedImage.userId,
                    relativePath = deletedImage.path,
                ),
            )
            if (result is ImageStorageGateway.DeleteResult.Failure) {
                throw IOException("画像ファイルの削除に失敗しました: ${result.cause.message}", result.cause)
            }
        }.onFailure { e ->
            TraceLogger.impl().noticeThrowable(e, true)
        }
    }

    class DeletedImage(
        val userId: UserId,
        val path: String,
        val storageType: String?,
    )
}
