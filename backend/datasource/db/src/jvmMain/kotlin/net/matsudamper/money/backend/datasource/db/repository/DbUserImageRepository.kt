package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.app.interfaces.UserImageRepository
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL

class DbUserImageRepository : UserImageRepository {
    private val userImagesTable = DSL.table(DSL.name("user_images"))
    private val userIdField = DSL.field(DSL.name("user_id"), Int::class.java)
    private val imageHashField = DSL.field(DSL.name("image_hash"), String::class.java)
    private val imagePathField = DSL.field(DSL.name("image_path"), String::class.java)

    override fun saveImage(
        userId: UserId,
        imageHash: String,
        relativePath: String,
    ): Boolean {
        return runCatching {
            DbConnectionImpl.use { connection ->
                DSL.using(connection)
                    .insertInto(userImagesTable)
                    .set(userIdField, userId.value)
                    .set(imageHashField, imageHash)
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
        imageHash: String,
    ): String? {
        return DbConnectionImpl.use { connection ->
            DSL.using(connection)
                .select(imagePathField)
                .from(userImagesTable)
                .where(
                    userIdField.eq(userId.value)
                        .and(imageHashField.eq(imageHash)),
                )
                .limit(1)
                .fetchOne(imagePathField)
        }
    }
}
