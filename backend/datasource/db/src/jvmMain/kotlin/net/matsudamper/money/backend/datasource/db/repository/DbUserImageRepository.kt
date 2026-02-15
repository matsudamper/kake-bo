package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.app.interfaces.UserImageRepository
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.db.schema.tables.JUserImages
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL

class DbUserImageRepository : UserImageRepository {
    private val jUserImages = JUserImages.USER_IMAGES

    override fun saveImage(
        userId: UserId,
        displayId: String,
        relativePath: String,
        contentType: String,
    ): ImageId? {
        return runCatching<ImageId?> {
            DbConnectionImpl.use { connection ->
                val context = DSL.using(connection)
                val insertedCount = context
                    .insertInto(jUserImages)
                    .set(jUserImages.USER_ID, userId.value)
                    .set(jUserImages.DISPLAY_ID, displayId)
                    .set(jUserImages.IMAGE_PATH, relativePath)
                    .set(jUserImages.CONTENT_TYPE, contentType)
                    .set(jUserImages.UPLOADED, false)
                    .execute()
                if (insertedCount != 1) {
                    return@use null
                }
                context
                    .select(
                        jUserImages.USER_IMAGE_ID,
                    )
                    .from(jUserImages)
                    .where(
                        jUserImages.USER_ID.eq(userId.value)
                            .and(jUserImages.DISPLAY_ID.eq(displayId)),
                    )
                    .orderBy(jUserImages.USER_IMAGE_ID.desc())
                    .limit(1)
                    .fetchOne()
                    ?.let { record ->
                        record.get(jUserImages.USER_IMAGE_ID)
                            ?.let { ImageId(it) }
                    }
            }
        }.getOrNull()
    }

    override fun deleteImage(
        userId: UserId,
        imageId: ImageId,
    ) {
        DbConnectionImpl.use { connection ->
            DSL.using(connection)
                .deleteFrom(jUserImages)
                .where(jUserImages.USER_IMAGE_ID.eq(imageId.value))
                .and(jUserImages.USER_ID.eq(userId.value))
                .execute()
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

    override fun getDisplayIdsByImageIds(
        userId: UserId,
        imageIds: List<ImageId>,
    ): Map<ImageId, String> {
        if (imageIds.isEmpty()) {
            return emptyMap()
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
