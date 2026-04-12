package net.matsudamper.money.frontend.common.base.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.matsudamper.money.element.MoneyUsageId

public object EmptyNotificationUsageRepository : NotificationUsageRepository {
    override fun notificationsFlow(): Flow<List<NotificationUsageRecord>> = flowOf(emptyList())

    override fun unaddedMatchedNotificationsFlow(): Flow<List<NotificationUsageMatchedRecord>> = flowOf(emptyList())

    override fun notAddedNotificationsFlow(): Flow<List<NotificationUsageRecord>> = flowOf(emptyList())

    override fun addedNotificationsFlow(): Flow<List<NotificationUsageRecord>> = flowOf(emptyList())

    override fun notificationDetailFlow(notificationKey: String): Flow<NotificationUsageDetail?> = flowOf(null)

    override suspend fun upsertNotification(record: NotificationUsageRecordInput): String {
        return record.notificationKey
    }

    override suspend fun markNotificationAsAdded(notificationKey: String, moneyUsageId: MoneyUsageId?) {
    }
}
