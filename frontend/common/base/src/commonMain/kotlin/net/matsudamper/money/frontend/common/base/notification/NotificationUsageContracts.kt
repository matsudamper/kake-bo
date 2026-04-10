package net.matsudamper.money.frontend.common.base.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDateTime
import net.matsudamper.money.element.MoneyUsageSubCategoryId

public data class NotificationUsageRecord(
    val notificationKey: String,
    val packageName: String,
    val text: String,
    val postedAtEpochMillis: Long,
    val receivedAtEpochMillis: Long,
    val isAdded: Boolean = false,
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
    val matchDescription: String,
    val parseDescription: String,
)

public data class NotificationUsageMatchedRecord(
    val record: NotificationUsageRecord,
    val draft: NotificationUsageDraft,
)

public interface NotificationUsageRepository {
    public fun matchedNotificationsFlow(): Flow<List<NotificationUsageMatchedRecord>>

    public fun unmatchedNotificationsFlow(): Flow<List<NotificationUsageRecord>>

    public suspend fun upsertNotification(record: NotificationUsageRecordInput)

    public suspend fun markNotificationAsAdded(notificationKey: String)
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
    override fun matchedNotificationsFlow(): Flow<List<NotificationUsageMatchedRecord>> = flowOf(emptyList())

    override fun unmatchedNotificationsFlow(): Flow<List<NotificationUsageRecord>> = flowOf(emptyList())

    override suspend fun upsertNotification(record: NotificationUsageRecordInput) {
    }

    override suspend fun markNotificationAsAdded(notificationKey: String) {
    }
}

public object EmptyNotificationUsageAccessGateway : NotificationUsageAccessGateway {
    override fun accessStateFlow(): Flow<NotificationAccessState> = flowOf(NotificationAccessState.Unsupported)

    override fun openAccessSettings() {
    }
}
