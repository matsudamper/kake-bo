package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageMatchedRecord
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecordInput
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRepository

internal class NotificationUsageRepositoryAndroidImpl(
    private val dao: NotificationUsageDao,
    private val parsers: List<NotificationUsageParser>,
) : NotificationUsageRepository {
    override fun matchedNotificationsFlow(): Flow<List<NotificationUsageMatchedRecord>> {
        return dao.observeAll().map { entities ->
            entities.mapNotNull { entity ->
                val record = entity.toRecord()
                parsers.firstNotNullOfOrNull { parser ->
                    parser.parse(record)?.let { draft ->
                        NotificationUsageMatchedRecord(
                            record = record,
                            draft = draft,
                        )
                    }
                }
            }
        }
    }

    override fun unmatchedNotificationsFlow(): Flow<List<NotificationUsageRecord>> {
        return dao.observeAll().map { entities ->
            entities.mapNotNull { entity ->
                val record = entity.toRecord()
                record.takeIf { it.matchesAnyParser().not() }
            }
        }
    }

    override suspend fun upsertNotification(record: NotificationUsageRecordInput) {
        val isAdded = dao.findByKey(record.notificationKey)?.isAdded ?: false
        dao.upsert(
            NotificationUsageEntity(
                notificationKey = record.notificationKey,
                packageName = record.packageName,
                text = record.text,
                postedAtEpochMillis = record.postedAtEpochMillis,
                receivedAtEpochMillis = record.receivedAtEpochMillis,
                isAdded = isAdded,
            ),
        )
    }

    override suspend fun markNotificationAsAdded(notificationKey: String) {
        dao.markAsAdded(notificationKey)
    }

    private fun NotificationUsageEntity.toRecord(): NotificationUsageRecord {
        return NotificationUsageRecord(
            notificationKey = notificationKey,
            packageName = packageName,
            text = text,
            postedAtEpochMillis = postedAtEpochMillis,
            receivedAtEpochMillis = receivedAtEpochMillis,
            isAdded = isAdded,
        )
    }

    private fun NotificationUsageRecord.matchesAnyParser(): Boolean {
        return parsers.any { parser -> parser.parse(this) != null }
    }
}
