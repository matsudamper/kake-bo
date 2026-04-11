package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageDetail
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageMatchedRecord
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecordInput
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRepository

internal class NotificationUsageRepositoryAndroidImpl(
    private val dao: NotificationUsageDao,
    private val parsers: List<NotificationUsageParser>,
) : NotificationUsageRepository {
    override fun notificationsFlow(): Flow<List<NotificationUsageRecord>> {
        return dao.observeAll().map { entities ->
            entities.map { entity ->
                entity.toRecord()
            }
        }
    }

    override fun unaddedMatchedNotificationsFlow(): Flow<List<NotificationUsageMatchedRecord>> {
        return dao.observeNotAdded().map { entities ->
            entities.mapNotNull { entity ->
                val record = entity.toRecord()
                record.toMatchedRecord()
            }
        }
    }

    override fun notAddedNotificationsFlow(): Flow<List<NotificationUsageRecord>> {
        return dao.observeNotAdded().map { entities ->
            entities.map { entity ->
                entity.toRecord()
            }
        }
    }

    override fun addedNotificationsFlow(): Flow<List<NotificationUsageRecord>> {
        return dao.observeAdded().map { entities ->
            entities.map { entity ->
                entity.toRecord()
            }
        }
    }

    override fun notificationDetailFlow(notificationKey: String): Flow<NotificationUsageDetail?> {
        return dao.observeByKey(notificationKey).map { entity ->
            val record = entity?.toRecord() ?: return@map null
            NotificationUsageDetail(
                record = record,
                matched = record.toMatchedRecord(),
            )
        }
    }

    override suspend fun upsertNotification(record: NotificationUsageRecordInput): String {
        val notificationKey = record.resolveNotificationKey()
        dao.insert(
            NotificationUsageEntity(
                notificationKey = notificationKey,
                packageName = record.packageName,
                text = record.text,
                postedAtEpochMillis = record.postedAtEpochMillis,
                receivedAtEpochMillis = record.receivedAtEpochMillis,
            ),
        )
        return notificationKey
    }

    override suspend fun markNotificationAsAdded(notificationKey: String, moneyUsageId: MoneyUsageId?) {
        dao.markAsAdded(notificationKey, moneyUsageId?.id)
    }

    private fun NotificationUsageEntity.toRecord(): NotificationUsageRecord {
        return NotificationUsageRecord(
            notificationKey = notificationKey,
            packageName = packageName,
            text = text,
            postedAtEpochMillis = postedAtEpochMillis,
            receivedAtEpochMillis = receivedAtEpochMillis,
            isAdded = isAdded,
            moneyUsageId = moneyUsageId?.let { MoneyUsageId(it) },
        )
    }

    private suspend fun NotificationUsageRecordInput.resolveNotificationKey(): String {
        val legacyEntity = dao.findByKey(notificationKey)
        if (legacyEntity != null && legacyEntity.hasSameNotificationValue(this)) {
            return notificationKey
        }
        return NotificationUsageKeyBuilder.build(
            notificationKey = notificationKey,
            packageName = packageName,
            text = text,
            postedAtEpochMillis = postedAtEpochMillis,
        )
    }

    private fun NotificationUsageEntity.hasSameNotificationValue(record: NotificationUsageRecordInput): Boolean {
        return packageName == record.packageName &&
            text == record.text &&
            postedAtEpochMillis == record.postedAtEpochMillis
    }

    private fun NotificationUsageRecord.toMatchedRecord(): NotificationUsageMatchedRecord? {
        return parsers.firstNotNullOfOrNull { parser ->
            val draft = parser.parse(this) ?: return@firstNotNullOfOrNull null
            NotificationUsageMatchedRecord(
                record = this,
                draft = draft,
                filterDefinition = parser.filterDefinition,
            )
        }
    }
}
