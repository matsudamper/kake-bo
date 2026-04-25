package net.matsudamper.money.backend.datasource.db.repository

import java.io.File
import java.io.IOException
import net.matsudamper.money.backend.app.interfaces.AdminImageRepository
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.base.TraceLogger
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
                        )
                        .from(userImages)
                        .where(
                            userImages.USER_IMAGE_ID.`in`(uniqueImageIds),
                        )
                        .fetch()

                    for (record in records) {
                        val relativePath = record.get(userImages.IMAGE_PATH)
                            ?: throw IllegalStateException("画像パスが見つかりませんでした: ${userImages.USER_IMAGE_ID.name}=${record.get(userImages.USER_IMAGE_ID)}")
                        val imageFile = File(ServerEnv.imageStoragePath, relativePath)
                        if (imageFile.exists().not()) continue
                        if (imageFile.delete().not()) {
                            throw IOException("ファイルの削除に失敗しました: $imageFile")
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

    /**
     * IMAGE_PATHは「YYYY-MM/filename」形式で保存されている。
     * 先頭7文字（YYYY-MM部分）でグループ化して月別の未紐づき画像数を返す。
     */
    override fun getImageDirectoryMonths(): List<AdminImageRepository.ImageDirectoryMonth> {
        return DbConnectionImpl.use { connection ->
            val yearMonthField = DSL.substring(
                userImages.IMAGE_PATH,
                YEAR_MONTH_START_INDEX,
                YEAR_MONTH_LENGTH,
            )
            DSL.using(connection)
                .select(
                    yearMonthField.`as`("year_month"),
                    DSL.count().`as`("cnt"),
                )
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
                .groupBy(yearMonthField)
                .orderBy(yearMonthField.desc())
                .fetch()
                .map { record ->
                    AdminImageRepository.ImageDirectoryMonth(
                        yearMonth = record.get("year_month", String::class.java)!!,
                        count = record.get("cnt", Int::class.java)!!,
                    )
                }
        }
    }

    override fun getUnlinkedImagesByMonth(yearMonth: String): List<AdminImageRepository.Item> {
        require(YEAR_MONTH_PATTERN.matches(yearMonth)) { "yearMonth must be in YYYY-MM format" }
        return DbConnectionImpl.use { connection ->
            DSL.using(connection)
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
                .where(
                    userImages.UPLOADED.eq(true)
                        .and(usageImagesRelation.USER_IMAGE_ID.isNull)
                        .and(userImages.IMAGE_PATH.like("$yearMonth/%")),
                )
                .orderBy(userImages.USER_IMAGE_ID.desc())
                .fetch()
                .map { record ->
                    AdminImageRepository.Item(
                        imageId = ImageId(record.get(userImages.USER_IMAGE_ID)!!),
                        displayId = record.get(userImages.DISPLAY_ID)!!,
                        userId = UserId(record.get(userImages.USER_ID)!!),
                        userName = record.get(users.USER_NAME)!!,
                    )
                }
        }
    }

    private companion object {
        /** IMAGE_PATHのYYYY-MM部分の開始位置（1-indexed） */
        private const val YEAR_MONTH_START_INDEX = 1

        /** IMAGE_PATHのYYYY-MM部分の長さ */
        private const val YEAR_MONTH_LENGTH = 7

        /** YYYY-MM形式の正規表現パターン */
        private val YEAR_MONTH_PATTERN = Regex("""\d{4}-\d{2}""")
    }
}
