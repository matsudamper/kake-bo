package net.matsudamper.money.frontend.feature.notification.viewmodel

import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageAccessGateway
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageAccessGateway.NotificationAccessState
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageMatchedRecord
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRepository
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.feature.notification.ui.NotificationUsageListScreenUiState

public class NotificationUsageViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val mode: Mode,
    private val repository: NotificationUsageRepository,
    private val accessGateway: NotificationUsageAccessGateway,
) : CommonViewModel(scopedObjectFeature) {
    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val statusStateFlow = MutableStateFlow(mode.initialStatus)
    private val searchQueryStateFlow = MutableStateFlow("")
    private val copyJsonFormatter = Json {
        prettyPrint = true
    }

    public enum class Mode {
        AddFromNotification,
        NotificationList,
    }

    public val uiStateFlow: StateFlow<NotificationUsageListScreenUiState> = MutableStateFlow(
        NotificationUsageListScreenUiState(
            title = mode.title,
            items = emptyList<NotificationUsageListScreenUiState.Item>().toImmutableList(),
            filters = statusStateFlow.value.toUiState().toImmutableList(),
            showSearch = mode == Mode.NotificationList,
            onSearchQueryChange = if (mode == Mode.NotificationList) {
                { query -> searchQueryStateFlow.value = query }
            } else {
                null
            },
            emptyText = mode.emptyText(statusStateFlow.value),
            accessSection = null,
            topBarActions = mode.topBarActions().toImmutableList(),
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navigateToHome()
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            combine(
                accessGateway.accessStateFlow(),
                statusStateFlow,
                searchQueryStateFlow,
                itemsFlow(),
            ) { accessState, status, searchQuery, items ->
                NotificationUsageUiStateSource(
                    accessState = accessState,
                    status = status,
                    searchQuery = searchQuery,
                    items = items,
                )
            }.collect { source ->
                uiStateFlow.update { uiState ->
                    val filteredItems = if (mode == Mode.NotificationList) {
                        source.items.filterByQuery(source.searchQuery)
                    } else {
                        source.items
                    }
                    uiState.copy(
                        items = filteredItems.map { it.toUiItem() }.toImmutableList(),
                        filters = source.status.toUiState().toImmutableList(),
                        emptyText = mode.emptyText(source.status),
                        accessSection = source.accessState.toAccessSection(),
                    )
                }
            }
        }
    }.asStateFlow()

    @Suppress("OPT_IN_USAGE")
    private fun itemsFlow(): Flow<List<ItemSource>> {
        return statusStateFlow.flatMapLatest { status ->
            when (status) {
                Status.All -> repository.notificationsFlow()
                    .map { records -> records.map { ItemSource.Raw(it) } }

                Status.NotAdded -> {
                    when (mode) {
                        Mode.AddFromNotification -> repository.unaddedMatchedNotificationsFlow()
                            .map { records -> records.map { ItemSource.Matched(it) } }

                        Mode.NotificationList -> repository.notAddedNotificationsFlow()
                            .map { records -> records.map { ItemSource.Raw(it) } }
                    }
                }

                Status.Added -> repository.addedNotificationsFlow()
                    .map { records -> records.map { ItemSource.Raw(it) } }
            }
        }
    }

    private fun Mode.topBarActions(): List<NotificationUsageListScreenUiState.TopBarAction> {
        return when (this) {
            Mode.AddFromNotification -> listOf(
                NotificationUsageListScreenUiState.TopBarAction(
                    label = "条件",
                    onClick = {
                        navigate(ScreenStructure.Root.Add.NotificationUsageFilters)
                    },
                ),
                NotificationUsageListScreenUiState.TopBarAction(
                    label = "一覧",
                    onClick = {
                        navigate(ScreenStructure.Root.Add.NotificationUsageDebug)
                    },
                ),
            )

            Mode.NotificationList -> listOf()
        }
    }

    private fun NotificationAccessState.toAccessSection(): NotificationUsageListScreenUiState.AccessSection? {
        return when (this) {
            NotificationAccessState.Granted -> null
            NotificationAccessState.NotGranted -> NotificationUsageListScreenUiState.AccessSection(
                title = "通知アクセスが必要です",
                description = "通知アクセスを有効にすると、通知から追加に使う一覧を取得できます。",
                buttonLabel = "通知アクセス設定を開く",
                onClickButton = accessGateway::openAccessSettings,
            )
        }
    }

    private fun ItemSource.toUiItem(): NotificationUsageListScreenUiState.Item {
        val source = this
        return when (source) {
            is ItemSource.Matched -> source.record.toUiItem(
                title = source.record.record.packageName,
                statusLabel = "未追加",
                description = source.record.record.text.ifBlank { "(テキストなし)" },
                onClickCopyJson = {
                    copyNotificationJson(source.toCopyJson())
                },
            )

            is ItemSource.Raw -> source.record.toUiItem(
                title = source.record.packageName,
                statusLabel = if (source.record.isAdded) "追加済み" else "未追加",
                description = source.record.text.ifBlank { "(テキストなし)" },
                onClickCopyJson = {
                    copyNotificationJson(source.toCopyJson())
                },
            )
        }
    }

    private fun NotificationUsageMatchedRecord.toUiItem(
        title: String,
        statusLabel: String,
        description: String,
        onClickCopyJson: () -> Unit,
    ): NotificationUsageListScreenUiState.Item {
        return NotificationUsageListScreenUiState.Item(
            title = title,
            receivedAt = Formatter.formatYearMonthDateTime(record.receivedAtEpochMillis.toLocalDateTime()),
            statusLabel = statusLabel,
            description = description,
            onClick = {
                navigate(
                    ScreenStructure.NotificationUsageDetail(
                        notificationUsageKey = record.notificationKey,
                    ),
                )
            },
            onClickCopyJson = onClickCopyJson,
        )
    }

    private fun NotificationUsageRecord.toUiItem(
        title: String,
        statusLabel: String,
        description: String,
        onClickCopyJson: () -> Unit,
    ): NotificationUsageListScreenUiState.Item {
        return NotificationUsageListScreenUiState.Item(
            title = title,
            receivedAt = Formatter.formatYearMonthDateTime(receivedAtEpochMillis.toLocalDateTime()),
            statusLabel = statusLabel,
            description = description,
            onClick = {
                navigate(
                    ScreenStructure.NotificationUsageDetail(
                        notificationUsageKey = notificationKey,
                    ),
                )
            },
            onClickCopyJson = onClickCopyJson,
        )
    }

    private fun ItemSource.toCopyJson(): String {
        return copyJsonFormatter.encodeToString(JsonObject.serializer(), toCopyJsonObject())
    }

    private fun ItemSource.toCopyJsonObject(): JsonObject {
        val record = when (this) {
            is ItemSource.Matched -> record.record
            is ItemSource.Raw -> record
        }
        return buildJsonObject {
            put("packageName", record.packageName)
            put("text", record.text)
        }
    }

    private fun Long.toLocalDateTime(): kotlinx.datetime.LocalDateTime {
        return Instant.fromEpochMilliseconds(this)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    }

    private fun navigate(structure: ScreenStructure) {
        viewModelScope.launch {
            eventSender.send {
                it.navigate(structure)
            }
        }
    }

    private fun navigateToHome() {
        viewModelScope.launch {
            eventSender.send {
                it.navigateToHome()
            }
        }
    }

    private fun copyNotificationJson(json: String) {
        viewModelScope.launch {
            eventSender.send {
                it.copyToClipboard(json)
                it.showToast("通知情報のJSONをコピーしました")
            }
        }
    }

    private fun Status.toUiState(): List<NotificationUsageListScreenUiState.Filter> {
        return when (mode) {
            Mode.AddFromNotification -> listOf(Status.NotAdded, Status.Added)
            Mode.NotificationList -> listOf(Status.All, Status.Added, Status.NotAdded)
        }.map { filter ->
            NotificationUsageListScreenUiState.Filter(
                label = filter.label,
                selected = filter == this,
                onClick = {
                    statusStateFlow.value = filter
                },
            )
        }
    }

    private data class NotificationUsageUiStateSource(
        val accessState: NotificationAccessState,
        val status: Status,
        val searchQuery: String,
        val items: List<ItemSource>,
    )

    private fun List<ItemSource>.filterByQuery(query: String): List<ItemSource> {
        if (query.isBlank()) return this
        val q = query.lowercase()
        return filter { source ->
            when (source) {
                is ItemSource.Matched -> {
                    source.record.record.packageName.lowercase().contains(q) ||
                            source.record.record.text.lowercase().contains(q)
                }

                is ItemSource.Raw -> {
                    source.record.packageName.lowercase().contains(q) ||
                            source.record.text.lowercase().contains(q)
                }
            }
        }
    }

    private sealed interface ItemSource {
        data class Matched(val record: NotificationUsageMatchedRecord) : ItemSource

        data class Raw(val record: NotificationUsageRecord) : ItemSource
    }

    public interface Event {
        public fun navigate(structure: ScreenStructure)

        public fun navigateToHome()

        public fun copyToClipboard(text: String)

        public fun showToast(text: String)
    }

    private val Mode.title: String
        get() = when (this) {
            Mode.AddFromNotification -> "通知から追加"
            Mode.NotificationList -> "通知一覧"
        }

    private val Mode.initialStatus: Status
        get() = when (this) {
            Mode.AddFromNotification -> Status.NotAdded
            Mode.NotificationList -> Status.All
        }

    private fun Mode.emptyText(status: Status): String {
        return when (this) {
            Mode.AddFromNotification -> status.addFromNotificationEmptyText
            Mode.NotificationList -> status.notificationListEmptyText
        }
    }

    private enum class Status(
        val label: String,
        val addFromNotificationEmptyText: String,
        val notificationListEmptyText: String,
    ) {
        All(
            label = "全て",
            addFromNotificationEmptyText = "",
            notificationListEmptyText = "通知はありません。",
        ),
        NotAdded(
            label = "未追加",
            addFromNotificationEmptyText = "一致する未追加の通知はありません。",
            notificationListEmptyText = "未追加の通知はありません。",
        ),
        Added(
            label = "追加済み",
            addFromNotificationEmptyText = "追加済みの通知はありません。",
            notificationListEmptyText = "追加済みの通知はありません。",
        ),
    }
}
