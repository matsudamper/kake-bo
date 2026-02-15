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
        imageId: ImageId,
        relativePath: String,
    ): Boolean {
        return runCatching {
            DbConnectionImpl.use { connection ->
                DSL.using(connection)
                    .insertInto(jUserImages)
                    .set(jUserImages.USER_ID, userId.value)
                    .set(jUserImages.DISPLAY_ID, imageId.value)
                    .set(jUserImages.IMAGE_PATH, relativePath)
                    .onDuplicateKeyIgnore()
                    .execute() == 1
            }
        }.fold(
            onSuccess = { it },
            onFailure = { false },
        )
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
                        .and(jUserImages.DISPLAY_ID.eq(imageId.value)),
                )
                .limit(1)
                .fetchOne(jUserImages.IMAGE_PATH)
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
                                .and(jUserImages.DISPLAY_ID.eq(imageId.value)),
                        ),
                )
        }
    }
}
