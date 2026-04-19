package net.matsudamper.money.frontend.common.base.notification

import kotlinx.coroutines.flow.Flow
import net.matsudamper.money.element.MoneyUsageId

public interface NotificationUsageRepository {
    public fun notificationsFlow(): Flow<List<NotificationUsageRecord>>

    public fun unaddedMatchedNotificationsFlow(): Flow<List<NotificationUsageMatchedRecord>>

    public fun notAddedNotificationsFlow(): Flow<List<NotificationUsageRecord>>

    public fun addedNotificationsFlow(): Flow<List<NotificationUsageRecord>>

    public fun notificationDetailFlow(notificationKey: NotificationUsageKey): Flow<NotificationUsageDetail?>

    public suspend fun upsertNotification(record: NotificationUsageRecordInput): NotificationUsageKey

    public suspend fun markNotificationAsAdded(notificationKey: NotificationUsageKey, moneyUsageId: MoneyUsageId?)
}
