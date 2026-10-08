package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.app.interfaces.DeleteUsageImageRelationDao
import net.matsudamper.money.backend.app.interfaces.ImageStorageGateway
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.db.schema.tables.JMoneyUsageImagesRelation
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.UserId
import org.jooq.TransactionalRunnable
import org.jooq.impl.DSL

class DeleteUsageImageRelationDaoImpl(
    localImageStorageGateway: ImageStorageGateway,
    s3ImageStorageGateway: ImageStorageGateway?,
) : DeleteUsageImageRelationDao {
    private val userImageDeleter = UserImageDeleter(
        localImageStorageGateway = localImageStorageGateway,
        s3ImageStorageGateway = s3ImageStorageGateway,
    )

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

                        userImageDeleter.delete(
                            context = DSL.using(connection),
                            userId = userId,
                            imageId = imageId,
                        )
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
