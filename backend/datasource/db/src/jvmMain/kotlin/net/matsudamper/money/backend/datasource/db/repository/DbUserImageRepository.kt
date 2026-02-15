package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.app.interfaces.UserImageRepository
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL

class DbUserImageRepository : UserImageRepository {
    private val userImagesTable = DSL.table(DSL.name("user_images"))
    private val userIdField = DSL.field(DSL.name("user_id"), Int::class.java)
    private val displayIdField = DSL.field(DSL.name("display_id"), String::class.java)
    private val imagePathField = DSL.field(DSL.name("image_path"), String::class.java)

    override fun saveImage(
        userId: UserId,
        imageId: ImageId,
        relativePath: String,
    ): Boolean {
        return runCatching {
            DbConnectionImpl.use { connection ->
                DSL.using(connection)
                    .insertInto(userImagesTable)
                    .set(userIdField, userId.value)
                    .set(displayIdField, imageId.value)
                    .set(imagePathField, relativePath)
                    .onDuplicateKeyUpdate()
                    .set(imagePathField, relativePath)
                    .execute() >= 1
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
                .select(imagePathField)
                .from(userImagesTable)
                .where(
                    userIdField.eq(userId.value)
                        .and(displayIdField.eq(imageId.value)),
                )
                .limit(1)
                .fetchOne(imagePathField)
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
                        .from(userImagesTable)
                        .where(
                            userIdField.eq(userId.value)
                                .and(displayIdField.eq(imageId.value)),
                        ),
                )
        }
    }
}
