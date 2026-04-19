package net.matsudamper.money.frontend.common.base.notification

import kotlinx.datetime.LocalDateTime
import net.matsudamper.money.element.MoneyUsageId

public data class NotificationUsageRecord(
    val notificationKey: NotificationUsageKey,
    val packageName: String,
    val text: String,
    val postedAtEpochMillis: Long,
    val receivedAtEpochMillis: Long,
    val isAdded: Boolean = false,
    val moneyUsageId: MoneyUsageId? = null,
    val notificationMetadata: String = "",
)

public data class NotificationUsageRecordInput(
    val notificationKey: NotificationUsageKey,
    val packageName: String,
    val text: String,
    val postedAtEpochMillis: Long,
    val receivedAtEpochMillis: Long,
    val notificationMetadata: String = "",
)

public data class NotificationUsageDraft(
    val title: String,
    val description: String,
    val amount: Int?,
    val dateTime: LocalDateTime,
)

public data class NotificationUsageFilterDefinition(
    val id: String,
    val title: String,
    val description: String,
)

public data class NotificationUsageMatchedRecord(
    val record: NotificationUsageRecord,
    val draft: NotificationUsageDraft,
    val filterDefinition: NotificationUsageFilterDefinition,
)

public data class NotificationUsageDetail(
    val record: NotificationUsageRecord,
    val matched: NotificationUsageMatchedRecord?,
)
