package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.app.interfaces.ImageStorageGateway
import net.matsudamper.money.backend.app.interfaces.UsageImageRelationDao
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.db.schema.tables.JMoneyUsageImagesRelation
import net.matsudamper.money.db.schema.tables.JMoneyUsages
import net.matsudamper.money.db.schema.tables.JUserImages
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.UserId
import org.jooq.DSLContext
import org.jooq.TransactionalCallable
import org.jooq.impl.DSL

class UsageImageRelationDaoImpl(
    localImageStorageGateway: ImageStorageGateway,
    s3ImageStorageGateway: ImageStorageGateway?,
) : UsageImageRelationDao {
    private val jUsage = JMoneyUsages.MONEY_USAGES
    private val jUsageImagesRelation = JMoneyUsageImagesRelation.MONEY_USAGE_IMAGES_RELATION
    private val jUserImages = JUserImages.USER_IMAGES
    private val userImageDeleter = UserImageDeleter(
        localImageStorageGateway = localImageStorageGateway,
        s3ImageStorageGateway = s3ImageStorageGateway,
    )

    override fun add(
        userId: UserId,
        moneyUsageId: MoneyUsageId,
        imageId: ImageId,
    ): Boolean {
        return runInTransaction { context ->
            verifyOwned(
                context = context,
                userId = userId,
                moneyUsageId = moneyUsageId,
                imageId = imageId,
            )
            insertToLast(
                context = context,
                userId = userId,
                moneyUsageId = moneyUsageId,
                imageId = imageId,
            )
            null
        }
    }

    override fun replace(
        userId: UserId,
        moneyUsageId: MoneyUsageId,
        oldImageId: ImageId,
        newImageId: ImageId,
    ): Boolean {
        return runInTransaction { context ->
            verifyOwned(
                context = context,
                userId = userId,
                moneyUsageId = moneyUsageId,
                imageId = newImageId,
            )
            val replacedCount = context
                .update(jUsageImagesRelation)
                .set(jUsageImagesRelation.USER_IMAGE_ID, newImageId.value)
                .where(
                    jUsageImagesRelation.USER_ID.eq(userId.value)
                        .and(jUsageImagesRelation.MONEY_USAGE_ID.eq(moneyUsageId.id))
                        .and(jUsageImagesRelation.USER_IMAGE_ID.eq(oldImageId.value)),
                )
                .execute()
            if (replacedCount == 0) {
                insertToLast(
                    context = context,
                    userId = userId,
                    moneyUsageId = moneyUsageId,
                    imageId = newImageId,
                )
                null
            } else {
                userImageDeleter.deleteRecordIfUnlinked(
                    context = context,
                    userId = userId,
                    imageId = oldImageId,
                )
            }
        }
    }

    /**
     * @param block 削除した画像があれば返す。ファイルはコミット後に削除する
     */
    private fun runInTransaction(block: (DSLContext) -> UserImageDeleter.DeletedImage?): Boolean {
        return runCatching {
            val deletedImage = DbConnectionImpl.use { connection ->
                DSL.using(connection).transactionResult(
                    TransactionalCallable { configuration ->
                        block(DSL.using(configuration))
                    },
                )
            }
            if (deletedImage != null) {
                userImageDeleter.deleteFile(deletedImage)
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

    private fun verifyOwned(
        context: DSLContext,
        userId: UserId,
        moneyUsageId: MoneyUsageId,
        imageId: ImageId,
    ) {
        val usageCount = context
            .selectCount()
            .from(jUsage)
            .where(
                jUsage.USER_ID.eq(userId.value)
                    .and(jUsage.MONEY_USAGE_ID.eq(moneyUsageId.id)),
            )
            .fetchOne(0, Int::class.java) ?: 0
        if (usageCount != 1) throw IllegalStateException("利用が見つかりませんでした: userId=${userId.value}, moneyUsageId=${moneyUsageId.id}")

        val imageCount = context
            .selectCount()
            .from(jUserImages)
            .where(
                jUserImages.USER_ID.eq(userId.value)
                    .and(jUserImages.USER_IMAGE_ID.eq(imageId.value)),
            )
            .fetchOne(0, Int::class.java) ?: 0
        if (imageCount != 1) throw IllegalStateException("ユーザー画像が見つかりませんでした: userId=${userId.value}, imageId=${imageId.value}")
    }

    private fun insertToLast(
        context: DSLContext,
        userId: UserId,
        moneyUsageId: MoneyUsageId,
        imageId: ImageId,
    ) {
        val maxOrder = context
            .select(DSL.max(jUsageImagesRelation.IMAGE_ORDER))
            .from(jUsageImagesRelation)
            .where(
                jUsageImagesRelation.USER_ID.eq(userId.value)
                    .and(jUsageImagesRelation.MONEY_USAGE_ID.eq(moneyUsageId.id)),
            )
            .fetchOne(0, Int::class.java)
        val nextOrder = if (maxOrder == null) 0 else maxOrder + 1

        context
            .insertInto(jUsageImagesRelation)
            .set(jUsageImagesRelation.USER_ID, userId.value)
            .set(jUsageImagesRelation.MONEY_USAGE_ID, moneyUsageId.id)
            .set(jUsageImagesRelation.USER_IMAGE_ID, imageId.value)
            .set(jUsageImagesRelation.IMAGE_ORDER, nextOrder)
            .onDuplicateKeyIgnore()
            .execute()
    }
}
