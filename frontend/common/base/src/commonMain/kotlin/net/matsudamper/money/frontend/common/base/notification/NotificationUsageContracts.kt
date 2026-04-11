package net.matsudamper.money.frontend.common.base.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
    val moneyUsageId: MoneyUsageId? = null,
)

public data class NotificationUsageRecordInput(
    val notificationKey: String,
    val packageName: String,
    val text: String,
    val postedAtEpochMillis: Long,
    val receivedAtEpochMillis: Long,
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

public interface NotificationUsageRepository {
    public fun notificationsFlow(): Flow<List<NotificationUsageRecord>>

    public fun unaddedMatchedNotificationsFlow(): Flow<List<NotificationUsageMatchedRecord>>

    public fun notAddedNotificationsFlow(): Flow<List<NotificationUsageRecord>>

    public fun addedNotificationsFlow(): Flow<List<NotificationUsageRecord>>

    public fun notificationDetailFlow(notificationKey: String): Flow<NotificationUsageDetail?>

    public suspend fun upsertNotification(record: NotificationUsageRecordInput): String

    public suspend fun markNotificationAsAdded(notificationKey: String, moneyUsageId: MoneyUsageId?)
}

public interface NotificationUsageParser {
    public val filterDefinition: NotificationUsageFilterDefinition

    public fun parse(record: NotificationUsageRecord): NotificationUsageDraft?
}

public interface NotificationUsageAccessGateway {
    public fun accessStateFlow(): Flow<NotificationAccessState>

    public fun openAccessSettings()
}

public sealed interface NotificationAccessState {
    public data object Unsupported : NotificationAccessState

    public data object Granted : NotificationAccessState

    public data object NotGranted : NotificationAccessState
}

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

public object EmptyNotificationUsageAccessGateway : NotificationUsageAccessGateway {
    override fun accessStateFlow(): Flow<NotificationAccessState> = flowOf(NotificationAccessState.Unsupported)

    override fun openAccessSettings() {
    }
}
