package net.matsudamper.money.backend.datasource.db.repository

import java.io.IOException
import net.matsudamper.money.backend.app.interfaces.AdminImageRepository
import net.matsudamper.money.backend.app.interfaces.ImageStorageGateway
import net.matsudamper.money.backend.app.interfaces.UserImageRepository
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.backend.datasource.db.element.DbStorageType
import net.matsudamper.money.db.schema.tables.JMoneyUsageImagesRelation
import net.matsudamper.money.db.schema.tables.JUserImages
import net.matsudamper.money.db.schema.tables.JUsers
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL

class DbAdminImageRepository(
    private val localImageStorageGateway: ImageStorageGateway,
    private val s3ImageStorageGateway: ImageStorageGateway?,
) : AdminImageRepository {
    private val userImages = JUserImages.USER_IMAGES
    private val users = JUsers.USERS
    private val usageImagesRelation = JMoneyUsageImagesRelation.MONEY_USAGE_IMAGES_RELATION

    override fun getUnlinkedImages(
        size: Int,
        cursor: AdminImageRepository.Cursor?,
    ): AdminImageRepository.Result {
        val fetchSize = size + 1
        return DbConnectionImpl.use { connection ->
            val condition = userImages.UPLOADED.eq(true)
                .and(usageImagesRelation.USER_IMAGE_ID.isNull)
                .let { condition ->
                    if (cursor == null) {
                        condition
                    } else {
                        condition.and(userImages.USER_IMAGE_ID.lt(cursor.imageId.value))
                    }
                }

            val records = DSL.using(connection)
                .select(
                    userImages.USER_IMAGE_ID,
                    userImages.DISPLAY_ID,
                    userImages.USER_ID,
                    userImages.IMAGE_PATH,
                    userImages.STORAGE_TYPE,
                    users.USER_NAME,
                )
                .from(userImages)
                .join(users)
                .on(users.USER_ID.eq(userImages.USER_ID))
                .leftJoin(usageImagesRelation)
                .on(
                    usageImagesRelation.USER_ID.eq(userImages.USER_ID)
                        .and(usageImagesRelation.USER_IMAGE_ID.eq(userImages.USER_IMAGE_ID)),
                )
                .where(condition)
                .orderBy(userImages.USER_IMAGE_ID.desc())
                .limit(fetchSize)
                .fetch()

            val items = records.take(size).map { record ->
                val storageTypeValue = record.get(userImages.STORAGE_TYPE)
                val storageType = when (storageTypeValue) {
                    DbStorageType.LOCAL.dbValue -> UserImageRepository.StorageType.LOCAL
                    DbStorageType.S3.dbValue -> UserImageRepository.StorageType.S3
                    else -> throw IllegalStateException("Unknown storage_type: $storageTypeValue")
                }
                AdminImageRepository.Item(
                    imageId = ImageId(record.get(userImages.USER_IMAGE_ID)!!),
                    displayId = record.get(userImages.DISPLAY_ID)!!,
                    userId = UserId(record.get(userImages.USER_ID)!!),
                    userName = record.get(users.USER_NAME)!!,
                    relativePath = record.get(userImages.IMAGE_PATH)!!,
                    storageType = storageType,
                )
            }
            AdminImageRepository.Result(
                items = items,
                cursor = if (records.size > size && items.isNotEmpty()) {
                    AdminImageRepository.Cursor(imageId = items.last().imageId)
                } else {
                    null
                },
            )
        }
    }

    override fun deleteImages(imageIds: List<ImageId>): Boolean {
        if (imageIds.isEmpty()) return true

        val uniqueImageIds = imageIds.map { it.value }.distinct()
        return runCatching {
            DbConnectionImpl.use { connection ->
                DSL.using(connection).transaction { configuration ->
                    val context = DSL.using(configuration)
                    val records = context
                        .select(
                            userImages.IMAGE_PATH,
                            userImages.STORAGE_TYPE,
                            userImages.USER_ID,
                            userImages.USER_IMAGE_ID,
                        )
                        .from(userImages)
                        .where(
                            userImages.USER_IMAGE_ID.`in`(uniqueImageIds),
                        )
                        .fetch()

                    for (record in records) {
                        val storageType = record.get(userImages.STORAGE_TYPE)
                        val relativePath = record.get(userImages.IMAGE_PATH)
                            ?: throw IllegalStateException("画像パスが見つかりませんでした: ${userImages.USER_IMAGE_ID.name}=${record.get(userImages.USER_IMAGE_ID)}")
                        val userId = UserId(record.get(userImages.USER_ID)!!)

                        when (storageType) {
                            DbStorageType.LOCAL.dbValue -> {
                                val result = localImageStorageGateway.delete(
                                    ImageStorageGateway.DeleteRequest(
                                        userId = userId,
                                        relativePath = relativePath,
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
                                        relativePath = relativePath,
                                    ),
                                )
                                if (result is ImageStorageGateway.DeleteResult.Failure) {
                                    throw IOException("S3オブジェクトの削除に失敗しました: ${result.cause.message}", result.cause)
                                }
                            }

                            else -> throw IllegalStateException("Unknown storage_type: $storageType")
                        }
                    }

                    context
                        .deleteFrom(usageImagesRelation)
                        .where(
                            usageImagesRelation.USER_IMAGE_ID.`in`(uniqueImageIds),
                        )
                        .execute()

                    context
                        .deleteFrom(userImages)
                        .where(
                            userImages.USER_IMAGE_ID.`in`(uniqueImageIds),
                        )
                        .execute()
                }
            }
        }.onFailure { throwable ->
            TraceLogger.impl().noticeThrowable(throwable, true)
        }.isSuccess
    }

    override fun countUnlinkedImages(): Int {
        return DbConnectionImpl.use { connection ->
            DSL.using(connection)
                .selectCount()
                .from(userImages)
                .leftJoin(usageImagesRelation)
                .on(
                    usageImagesRelation.USER_ID.eq(userImages.USER_ID)
                        .and(usageImagesRelation.USER_IMAGE_ID.eq(userImages.USER_IMAGE_ID)),
                )
                .where(
                    userImages.UPLOADED.eq(true)
                        .and(usageImagesRelation.USER_IMAGE_ID.isNull),
                )
                .fetchOne(DSL.count()) ?: 0
        }
    }

    /**
     * [JUserImages.UPLOADED]の状態は加味しない。アップロード中に失敗したゴミファイルも返す。
     */
    override fun getImageDataByDisplayId(displayId: String): AdminImageRepository.ImageData? {
        return DbConnectionImpl.use { connection ->
            DSL.using(connection)
                .select(
                    userImages.IMAGE_PATH,
                    userImages.CONTENT_TYPE,
                    userImages.USER_ID,
                    userImages.STORAGE_TYPE,
                )
                .from(userImages)
                .where(userImages.DISPLAY_ID.eq(displayId))
                .limit(1)
                .fetchOne()
                ?.let { record ->
                    val storageTypeValue = record.get(userImages.STORAGE_TYPE)
                    val storageType = when (storageTypeValue) {
                        DbStorageType.LOCAL.dbValue -> UserImageRepository.StorageType.LOCAL
                        DbStorageType.S3.dbValue -> UserImageRepository.StorageType.S3
                        else -> throw IllegalStateException("Unknown storage_type: $storageTypeValue")
                    }
                    AdminImageRepository.ImageData(
                        relativePath = record.get(userImages.IMAGE_PATH)!!,
                        contentType = record.get(userImages.CONTENT_TYPE)!!,
                        userId = UserId(record.get(userImages.USER_ID)!!),
                        storageType = storageType,
                    )
                }
        }
    }
}
