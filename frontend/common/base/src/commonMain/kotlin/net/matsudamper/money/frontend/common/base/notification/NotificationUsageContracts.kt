package net.matsudamper.money.frontend.common.base.notification

import kotlinx.datetime.LocalDateTime
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageSubCategoryId

public data class NotificationUsageRecord(
    val notificationKey: String,
    val packageName: String,
    val text: String,
    val postedAtEpochMillis: Long,
    val receivedAtEpochMillis: Long,
    val isAdded: Boolean = false,
    val moneyUsageIds: List<MoneyUsageId> = emptyList(),
    val notificationMetadata: String = "",
)

public data class NotificationUsageRecordInput(
    val notificationKey: String,
    val packageName: String,
    val text: String,
    val postedAtEpochMillis: Long,
    val receivedAtEpochMillis: Long,
    val notificationMetadata: String = "",
)

public data class NotificationUsageDraft(
    val title: String? = null,
    val description: String? = null,
    val amount: Int? = null,
    val dateTime: LocalDateTime? = null,
    val subCategoryId: MoneyUsageSubCategoryId? = null,
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
