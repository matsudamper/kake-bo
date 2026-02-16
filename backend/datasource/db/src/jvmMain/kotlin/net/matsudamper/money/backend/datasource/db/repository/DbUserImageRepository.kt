package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.app.interfaces.UserImageRepository
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.db.schema.tables.JMoneyUsageImagesRelation
import net.matsudamper.money.db.schema.tables.JUserImages
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL

class DbUserImageRepository : UserImageRepository {
    private val jUserImages = JUserImages.USER_IMAGES
    private val jUsageImagesRelation = JMoneyUsageImagesRelation.MONEY_USAGE_IMAGES_RELATION

    override fun saveImage(
        userId: UserId,
        displayId: String,
        relativePath: String,
        contentType: String,
    ): ImageId? {
        return runCatching<ImageId?> {
            DbConnectionImpl.use { connection ->
                val context = DSL.using(connection)
                context
                    .insertInto(jUserImages)
                    .set(jUserImages.USER_ID, userId.value)
                    .set(jUserImages.DISPLAY_ID, displayId)
                    .set(jUserImages.IMAGE_PATH, relativePath)
                    .set(jUserImages.CONTENT_TYPE, contentType)
                    .set(jUserImages.UPLOADED, false)
                    .returningResult(jUserImages.USER_IMAGE_ID)
                    .fetchOne()
                    ?.get(jUserImages.USER_IMAGE_ID)
                    ?.let { ImageId(it) }
            }
        }.getOrNull()
    }

    override fun deleteImage(
        userId: UserId,
        imageId: ImageId,
    ) {
        DbConnectionImpl.use { connection ->
            val context = DSL.using(connection)
            context.startTransaction()
            try {
                context
                    .deleteFrom(jUsageImagesRelation)
                    .where(
                        jUsageImagesRelation.USER_ID.eq(userId.value)
                            .and(jUsageImagesRelation.USER_IMAGE_ID.eq(imageId.value)),
                    )
                    .execute()
                context
                    .deleteFrom(jUserImages)
                    .where(
                        jUserImages.USER_IMAGE_ID.eq(imageId.value)
                            .and(jUserImages.USER_ID.eq(userId.value)),
                    )
                    .execute()
                context.commit()
            } catch (e: Throwable) {
                context.rollback()
                throw e
            }
        }
    }

    override fun markImageAsUploaded(userId: UserId, imageId: ImageId) {
        DbConnectionImpl.use { connection ->
            DSL.using(connection)
                .update(jUserImages)
                .set(jUserImages.UPLOADED, true)
                .where(jUserImages.USER_IMAGE_ID.eq(imageId.value))
                .and(jUserImages.USER_ID.eq(userId.value))
                .execute()
        }
    }

    override fun getImageDataByDisplayId(
        userId: UserId,
        displayId: String,
    ): UserImageRepository.ImageData? {
        return DbConnectionImpl.use { connection ->
            DSL.using(connection)
                .select(
                    jUserImages.IMAGE_PATH,
                    jUserImages.CONTENT_TYPE,
                )
                .from(jUserImages)
                .where(
                    jUserImages.USER_ID.eq(userId.value)
                        .and(jUserImages.DISPLAY_ID.eq(displayId)),
                )
                .limit(1)
                .fetchOne()
                ?.let { record ->
                    UserImageRepository.ImageData(
                        relativePath = record.get(jUserImages.IMAGE_PATH)!!,
                        contentType = record.get(jUserImages.CONTENT_TYPE)!!,
                    )
                }
        }
    }

    override fun exists(
        userId: UserId,
        imageId: ImageId,
    ): Boolean {
        return DbConnectionImpl.use { connection ->
            DSL.using(connection)
                .fetchExists(
                    DSL.using(connection)
                        .selectOne()
                        .from(jUserImages)
                        .where(
                            jUserImages.USER_ID.eq(userId.value)
                                .and(jUserImages.USER_IMAGE_ID.eq(imageId.value)),
                        ),
                )
        }
    }

    override fun getRelativePath(
        userId: UserId,
        imageId: ImageId,
    ): String? {
        return DbConnectionImpl.use { connection ->
            DSL.using(connection)
                .select(jUserImages.IMAGE_PATH)
                .from(jUserImages)
                .where(
                    jUserImages.USER_ID.eq(userId.value)
                        .and(jUserImages.USER_IMAGE_ID.eq(imageId.value)),
                )
                .limit(1)
                .fetchOne()
                ?.get(jUserImages.IMAGE_PATH)
        }
    }

    override fun countImageUsageRelations(
        userId: UserId,
        imageId: ImageId,
    ): Int {
        return DbConnectionImpl.use { connection ->
            DSL.using(connection)
                .selectCount()
                .from(jUsageImagesRelation)
                .where(
                    jUsageImagesRelation.USER_ID.eq(userId.value)
                        .and(jUsageImagesRelation.USER_IMAGE_ID.eq(imageId.value)),
                )
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    override fun deleteImageUsageRelation(
        userId: UserId,
        moneyUsageId: MoneyUsageId,
        imageId: ImageId,
    ) {
        DbConnectionImpl.use { connection ->
            DSL.using(connection)
                .deleteFrom(jUsageImagesRelation)
                .where(
                    jUsageImagesRelation.USER_ID.eq(userId.value)
                        .and(jUsageImagesRelation.MONEY_USAGE_ID.eq(moneyUsageId.id))
                        .and(jUsageImagesRelation.USER_IMAGE_ID.eq(imageId.value)),
                )
                .execute()
        }
    }

    override fun getDisplayIdsByImageIds(
        userId: UserId,
        imageIds: List<ImageId>,
    ): Map<ImageId, String> {
        if (imageIds.isEmpty()) {
            return mapOf()
        }
        val uniqueImageIds = imageIds.map { it.value }.distinct()
        return DbConnectionImpl.use { connection ->
            DSL.using(connection)
                .select(
                    jUserImages.USER_IMAGE_ID,
                    jUserImages.DISPLAY_ID,
                )
                .from(jUserImages)
                .where(
                    jUserImages.USER_ID.eq(userId.value)
                        .and(jUserImages.USER_IMAGE_ID.`in`(uniqueImageIds)),
                )
                .fetch()
                .associate { record ->
                    ImageId(record.get(jUserImages.USER_IMAGE_ID)!!) to record.get(jUserImages.DISPLAY_ID)!!
                }
        }
    }
}
