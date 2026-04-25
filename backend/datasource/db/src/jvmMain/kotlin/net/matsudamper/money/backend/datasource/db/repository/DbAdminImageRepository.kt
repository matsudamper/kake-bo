package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.app.interfaces.AdminImageRepository
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.db.schema.tables.JMoneyUsageImagesRelation
import net.matsudamper.money.db.schema.tables.JUserImages
import net.matsudamper.money.db.schema.tables.JUsers
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL

class DbAdminImageRepository : AdminImageRepository {
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
                AdminImageRepository.Item(
                    imageId = ImageId(record.get(userImages.USER_IMAGE_ID)!!),
                    displayId = record.get(userImages.DISPLAY_ID)!!,
                    userId = UserId(record.get(userImages.USER_ID)!!),
                    userName = record.get(users.USER_NAME)!!,
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
                )
                .from(userImages)
                .where(userImages.DISPLAY_ID.eq(displayId))
                .limit(1)
                .fetchOne()
                ?.let { record ->
                    AdminImageRepository.ImageData(
                        relativePath = record.get(userImages.IMAGE_PATH)!!,
                        contentType = record.get(userImages.CONTENT_TYPE)!!,
                    )
                }
        }
    }
}
