package net.matsudamper.money.frontend.common.base.notification

import kotlinx.coroutines.flow.Flow
import net.matsudamper.money.element.MoneyUsageId

public interface NotificationUsageRepository {
    public fun notificationsFlow(): Flow<List<NotificationUsageRecord>>

    public fun unaddedMatchedNotificationsFlow(): Flow<List<NotificationUsageMatchedRecord>>

    public fun notAddedNotificationsFlow(): Flow<List<NotificationUsageRecord>>

    public fun addedNotificationsFlow(): Flow<List<NotificationUsageRecord>>

    public fun notificationDetailFlow(notificationKey: String): Flow<NotificationUsageDetail?>

    public suspend fun upsertNotification(record: NotificationUsageRecordInput): String

    public suspend fun markNotificationAsAdded(notificationKey: String, moneyUsageId: MoneyUsageId?)
}
