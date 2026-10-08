package net.matsudamper.money.backend.datasource.db.repository

import java.io.IOException
import net.matsudamper.money.backend.app.interfaces.ImageStorageGateway
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
     * どの利用にも紐付いていない場合だけ、画像ファイルとレコードを削除する
     */
    fun deleteIfUnlinked(
        context: DSLContext,
        userId: UserId,
        imageId: ImageId,
    ) {
        val usageImagesRelation = JMoneyUsageImagesRelation.MONEY_USAGE_IMAGES_RELATION
        val linkedCount = context
            .selectCount()
            .from(usageImagesRelation)
            .where(
                usageImagesRelation.USER_ID.eq(userId.value)
                    .and(usageImagesRelation.USER_IMAGE_ID.eq(imageId.value)),
            )
            .fetchOne(0, Int::class.java) ?: 0
        if (linkedCount > 0) return

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

        when (storageType) {
            DbStorageType.LOCAL.dbValue -> {
                val result = localImageStorageGateway.delete(
                    ImageStorageGateway.DeleteRequest(
                        userId = userId,
                        relativePath = path,
                    ),
                )
                if (result is ImageStorageGateway.DeleteResult.Failure) {
                    throw IOException("ファイルの削除に失敗しました: ${result.cause.message}", result.cause)
                }
            }

            DbStorageType.S3.dbValue -> {
                val gateway = s3ImageStorageGateway
                    ?: throw IllegalStateException("S3ストレージゲートウェイが設定されていません")
                val result = gateway.delete(
                    ImageStorageGateway.DeleteRequest(
                        userId = userId,
                        relativePath = path,
                    ),
                )
                if (result is ImageStorageGateway.DeleteResult.Failure) {
                    throw IOException("S3オブジェクトの削除に失敗しました: ${result.cause.message}", result.cause)
                }
            }

            else -> throw IllegalStateException("Unknown storage_type: $storageType")
        }

        context
            .deleteFrom(userImages)
            .where(
                userImages.USER_ID.eq(userId.value)
                    .and(userImages.USER_IMAGE_ID.eq(imageId.value)),
            )
            .limit(1)
            .execute()
    }
}
