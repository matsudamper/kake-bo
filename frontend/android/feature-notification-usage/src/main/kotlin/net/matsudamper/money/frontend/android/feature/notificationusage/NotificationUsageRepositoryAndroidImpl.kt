package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageDetail
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageKey
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
                entityToRecord(entity)
            }
        }
    }

    override fun unaddedMatchedNotificationsFlow(): Flow<List<NotificationUsageMatchedRecord>> {
        return dao.observeNotAdded().map { entities ->
            entities.mapNotNull { entity ->
                val record = entityToRecord(entity)
                recordToMatchedRecord(record)
            }
        }
    }

    override fun notAddedNotificationsFlow(): Flow<List<NotificationUsageRecord>> {
        return dao.observeNotAdded().map { entities ->
            entities.map { entity ->
                entityToRecord(entity)
            }
        }
    }

    override fun addedNotificationsFlow(): Flow<List<NotificationUsageRecord>> {
        return dao.observeAdded().map { entities ->
            entities.map { entity ->
                entityToRecord(entity)
            }
        }
    }

    override fun notificationDetailFlow(notificationKey: NotificationUsageKey): Flow<NotificationUsageDetail?> {
        return dao.observeByKey(notificationKey.value).map { entity ->
            val record = if (entity != null) entityToRecord(entity) else return@map null
            NotificationUsageDetail(
                record = record,
                matched = recordToMatchedRecord(record),
            )
        }
    }

    override suspend fun upsertNotification(record: NotificationUsageRecordInput): NotificationUsageKey {
        val notificationKey = resolveNotificationKey(record)
        dao.insert(
            NotificationUsageEntity(
                notificationKey = notificationKey,
                packageName = record.packageName,
                text = record.text,
                postedAtEpochMillis = record.postedAtEpochMillis,
                receivedAtEpochMillis = record.receivedAtEpochMillis,
                notificationMetadata = record.notificationMetadata,
            ),
        )
        return NotificationUsageKey(notificationKey)
    }

    override suspend fun markNotificationAsAdded(notificationKey: NotificationUsageKey, moneyUsageId: MoneyUsageId?) {
        dao.markAsAdded(notificationKey.value, moneyUsageId?.id)
    }

    private fun entityToRecord(entity: NotificationUsageEntity): NotificationUsageRecord {
        return NotificationUsageRecord(
            notificationKey = NotificationUsageKey(entity.notificationKey),
            packageName = entity.packageName,
            text = entity.text,
            postedAtEpochMillis = entity.postedAtEpochMillis,
            receivedAtEpochMillis = entity.receivedAtEpochMillis,
            isAdded = entity.isAdded,
            moneyUsageId = entity.moneyUsageId?.let { MoneyUsageId(it) },
            notificationMetadata = entity.notificationMetadata,
        )
    }

    private suspend fun resolveNotificationKey(record: NotificationUsageRecordInput): String {
        val legacyEntity = dao.findByKey(record.notificationKey.value)
        if (legacyEntity != null && hasSameNotificationValue(legacyEntity, record)) {
            return record.notificationKey.value
        }
        return NotificationUsageKeyBuilder.build(
            notificationKey = record.notificationKey.value,
            packageName = record.packageName,
            text = record.text,
            postedAtEpochMillis = record.postedAtEpochMillis,
        )
    }

    private fun hasSameNotificationValue(entity: NotificationUsageEntity, record: NotificationUsageRecordInput): Boolean {
        return entity.packageName == record.packageName &&
            entity.text == record.text &&
            entity.postedAtEpochMillis == record.postedAtEpochMillis
    }

    private fun recordToMatchedRecord(record: NotificationUsageRecord): NotificationUsageMatchedRecord? {
        return parsers.firstNotNullOfOrNull { parser ->
            val draft = parser.parse(record) ?: return@firstNotNullOfOrNull null
            NotificationUsageMatchedRecord(
                record = record,
                draft = draft,
                filterDefinition = parser.filterDefinition,
            )
        }
    }
}
