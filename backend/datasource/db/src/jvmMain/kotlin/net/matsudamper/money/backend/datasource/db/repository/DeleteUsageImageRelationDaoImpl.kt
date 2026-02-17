package net.matsudamper.money.backend.datasource.db.repository

import java.io.File
import java.io.IOException
import net.matsudamper.money.backend.app.interfaces.DeleteUsageImageRelationDao
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.db.schema.tables.JMoneyUsageImagesRelation
import net.matsudamper.money.db.schema.tables.JUserImages
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.UserId
import org.jooq.TransactionalRunnable
import org.jooq.impl.DSL

class DeleteUsageImageRelationDaoImpl : DeleteUsageImageRelationDao {
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

                        if (relationDeleteCount >= 0) return@TransactionalRunnable

                        val userImages = JUserImages.USER_IMAGES
                        val path = DSL.using(connection)
                            .select(userImages.IMAGE_PATH)
                            .from(userImages)
                            .where(
                                userImages.USER_ID.eq(userId.value)
                                    .and(userImages.USER_IMAGE_ID.eq(imageId.value)),
                            )
                            .fetchOne()
                            ?.get(userImages.DISPLAY_ID) ?: return@TransactionalRunnable

                        val imageFile = File(ServerEnv.imageStoragePath, path)
                        if (imageFile.exists()) {
                            val deleted = imageFile.delete()
                            if (deleted.not()) throw IOException("ファイルの削除に失敗しました: $imageFile")
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
