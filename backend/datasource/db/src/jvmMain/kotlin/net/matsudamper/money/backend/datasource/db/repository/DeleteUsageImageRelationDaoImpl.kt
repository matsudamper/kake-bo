package net.matsudamper.money.backend.datasource.db.repository

import java.io.IOException
import net.matsudamper.money.backend.app.interfaces.DeleteUsageImageRelationDao
import net.matsudamper.money.backend.app.interfaces.ImageStorageGateway
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.backend.datasource.db.element.DbStorageType
import net.matsudamper.money.db.schema.tables.JMoneyUsageImagesRelation
import net.matsudamper.money.db.schema.tables.JUserImages
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.UserId
import org.jooq.TransactionalRunnable
import org.jooq.impl.DSL

class DeleteUsageImageRelationDaoImpl(
    private val localImageStorageGateway: ImageStorageGateway,
    private val s3ImageStorageGateway: ImageStorageGateway?,
) : DeleteUsageImageRelationDao {
    override fun delete(
        userId: UserId,
        moneyUsageId: MoneyUsageId,
        imageId: ImageId,
    ): Boolean {
        val usageImagesRelation = JMoneyUsageImagesRelation.MONEY_USAGE_IMAGES_RELATION
        return runCatching {
            DbConnectionImpl.use { connection ->
                DSL.using(connection).transaction(
                    TransactionalRunnable {
                        val relationDeleteCount = DSL.using(it)
                            .deleteFrom(usageImagesRelation)
                            .where(
                                usageImagesRelation.USER_ID.eq(userId.value)
                                    .and(usageImagesRelation.MONEY_USAGE_ID.eq(moneyUsageId.id))
                                    .and(usageImagesRelation.USER_IMAGE_ID.eq(imageId.value)),
                            )
                            .limit(1)
                            .execute()

                        if (relationDeleteCount <= 0) throw IllegalStateException("削除対象の関連が見つかりませんでした: userId=${userId.value}, moneyUsageId=${moneyUsageId.id}, imageId=${imageId.value}")

                        val userImages = JUserImages.USER_IMAGES
                        val record = DSL.using(connection)
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

                        DSL.using(connection)
                            .deleteFrom(userImages)
                            .where(
                                userImages.USER_ID.eq(userId.value)
                                    .and(userImages.USER_IMAGE_ID.eq(imageId.value)),
                            )
                            .limit(1)
                            .execute()
                    },
                )
            }
        }
            .onFailure { e ->
                TraceLogger.impl().noticeThrowable(e, true)
            }
            .fold(
                onSuccess = { true },
                onFailure = { false },
            )
    }
}
