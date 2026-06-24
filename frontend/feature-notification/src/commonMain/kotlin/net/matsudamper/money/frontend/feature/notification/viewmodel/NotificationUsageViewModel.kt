package net.matsudamper.money.frontend.feature.notification.viewmodel

import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
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

    private val viewModelStateFlow = MutableStateFlow(
        ViewModelState(
            status = initialStatus(mode),
            accessState = NotificationAccessState.NotGranted,
            itemsLoadingState = ViewModelState.ItemsLoadingState.Loading,
        ),
    )
    private val copyJsonFormatter = Json {
        prettyPrint = true
    }

    public val uiStateFlow: StateFlow<NotificationUsageListScreenUiState> = MutableStateFlow(
        NotificationUsageListScreenUiState(
            title = modeTitle(mode),
            itemsState = NotificationUsageListScreenUiState.ItemsState.Loading,
            filters = createStatusFilters(initialStatus(mode)).toImmutableList(),
            searchListener = createSearchListener(mode),
            accessSection = null,
            topBarActions = createTopBarActions(mode).toImmutableList(),
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navigateToHome()
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update { uiState ->
                    val itemsState = when (val loadingState = viewModelState.itemsLoadingState) {
                        ViewModelState.ItemsLoadingState.Loading -> {
                            NotificationUsageListScreenUiState.ItemsState.Loading
                        }
                        is ViewModelState.ItemsLoadingState.Loaded -> {
                            val filteredItems = if (mode == Mode.NotificationList) {
                                filterByQuery(loadingState.items, viewModelState.searchQuery)
                            } else {
                                loadingState.items
                            }
                            NotificationUsageListScreenUiState.ItemsState.Loaded(
                                items = filteredItems.map { createUiItem(it) }.toImmutableList(),
                                emptyText = emptyText(mode, viewModelState.status),
                            )
                        }
                    }
                    uiState.copy(
                        itemsState = itemsState,
                        filters = createStatusFilters(viewModelState.status).toImmutableList(),
                        accessSection = createAccessSection(viewModelState.accessState),
                    )
                }
            }
        }
    }.asStateFlow()

    init {
        viewModelScope.launch {
            accessGateway.accessStateFlow().collectLatest { accessState ->
                viewModelStateFlow.update { it.copy(accessState = accessState) }
            }
        }
        viewModelScope.launch {
            viewModelStateFlow
                .map { it.status }
                .distinctUntilChanged()
                .collectLatest { status ->
                    viewModelStateFlow.update { it.copy(itemsLoadingState = ViewModelState.ItemsLoadingState.Loading) }
                    itemsFlow(status).collectLatest { items ->
                        viewModelStateFlow.update {
                            it.copy(itemsLoadingState = ViewModelState.ItemsLoadingState.Loaded(items))
                        }
                    }
                }
        }
    }

    private fun itemsFlow(status: Status): Flow<List<ViewModelState.ItemSource>> {
        return when (status) {
            Status.All -> repository.notificationsFlow()
                .map { records -> records.map { ViewModelState.ItemSource.Raw(it) } }

            Status.NotAdded -> {
                when (mode) {
                    Mode.AddFromNotification -> repository.unaddedMatchedNotificationsFlow()
                        .map { records -> records.map { ViewModelState.ItemSource.Matched(it) } }

                    Mode.NotificationList -> repository.notAddedNotificationsFlow()
                        .map { records -> records.map { ViewModelState.ItemSource.Raw(it) } }
                }
            }

            Status.Added -> repository.addedNotificationsFlow()
                .map { records -> records.map { ViewModelState.ItemSource.Raw(it) } }
        }
    }

    private fun createTopBarActions(mode: Mode): List<NotificationUsageListScreenUiState.TopBarAction> {
        return when (mode) {
            Mode.AddFromNotification -> listOf(
                NotificationUsageListScreenUiState.TopBarAction(
                    label = "条件",
                    listener = object : NotificationUsageListScreenUiState.TopBarActionListener {
                        override fun onClick() {
                            navigate(ScreenStructure.Root.Add.NotificationUsageFilters)
                        }
                    },
                ),
                NotificationUsageListScreenUiState.TopBarAction(
                    label = "一覧",
                    listener = object : NotificationUsageListScreenUiState.TopBarActionListener {
                        override fun onClick() {
                            navigate(ScreenStructure.Root.Add.NotificationUsageDebug)
                        }
                    },
                ),
            )

            Mode.NotificationList -> listOf()
        }
    }

    private fun createAccessSection(accessState: NotificationAccessState): NotificationUsageListScreenUiState.AccessSection? {
        return when (accessState) {
            NotificationAccessState.Granted -> null
            NotificationAccessState.NotGranted -> NotificationUsageListScreenUiState.AccessSection(
                title = "通知アクセスが必要です",
                description = "通知アクセスを有効にすると、通知から追加に使う一覧を取得できます。",
                buttonLabel = "通知アクセス設定を開く",
                listener = object : NotificationUsageListScreenUiState.AccessSectionListener {
                    override fun onClickButton() {
                        accessGateway.openAccessSettings()
                    }
                },
            )
        }
    }

    private fun createUiItem(source: ViewModelState.ItemSource): NotificationUsageListScreenUiState.Item {
        return when (source) {
            is ViewModelState.ItemSource.Matched -> createMatchedUiItem(
                record = source.record,
                title = source.record.record.packageName,
                statusLabel = "未追加",
                description = source.record.record.text.ifBlank { "(テキストなし)" },
                copyJson = createCopyJson(source),
            )

            is ViewModelState.ItemSource.Raw -> createRawUiItem(
                record = source.record,
                title = source.record.packageName,
                statusLabel = if (source.record.isAdded) "追加済み" else "未追加",
                description = source.record.text.ifBlank { "(テキストなし)" },
                copyJson = createCopyJson(source),
            )
        }
    }

    private fun createMatchedUiItem(
        record: NotificationUsageMatchedRecord,
        title: String,
        statusLabel: String,
        description: String,
        copyJson: String,
    ): NotificationUsageListScreenUiState.Item {
        return NotificationUsageListScreenUiState.Item(
            title = title,
            receivedAt = Formatter.formatYearMonthDateTime(
                toLocalDateTime(record.record.receivedAtEpochMillis),
            ),
            statusLabel = statusLabel,
            description = description,
            listener = object : NotificationUsageListScreenUiState.ItemListener {
                override fun onClick() {
                    navigate(
                        ScreenStructure.NotificationUsageDetail(
                            notificationUsageKey = record.record.notificationKey,
                        ),
                    )
                }

                override fun onClickCopyJson() {
                    copyNotificationJson(copyJson)
                }
            },
        )
    }

    private fun createRawUiItem(
        record: NotificationUsageRecord,
        title: String,
        statusLabel: String,
        description: String,
        copyJson: String,
    ): NotificationUsageListScreenUiState.Item {
        return NotificationUsageListScreenUiState.Item(
            title = title,
            receivedAt = Formatter.formatYearMonthDateTime(
                toLocalDateTime(record.receivedAtEpochMillis),
            ),
            statusLabel = statusLabel,
            description = description,
            listener = object : NotificationUsageListScreenUiState.ItemListener {
                override fun onClick() {
                    navigate(
                        ScreenStructure.NotificationUsageDetail(
                            notificationUsageKey = record.notificationKey,
                        ),
                    )
                }

                override fun onClickCopyJson() {
                    copyNotificationJson(copyJson)
                }
            },
        )
    }

    private fun createCopyJson(source: ViewModelState.ItemSource): String {
        return copyJsonFormatter.encodeToString(JsonObject.serializer(), createCopyJsonObject(source))
    }

    private fun createCopyJsonObject(source: ViewModelState.ItemSource): JsonObject {
        val record = when (source) {
            is ViewModelState.ItemSource.Matched -> source.record.record
            is ViewModelState.ItemSource.Raw -> source.record
        }
        return buildJsonObject {
            put("packageName", record.packageName)
            put("text", record.text)
        }
    }

    private fun toLocalDateTime(epochMillis: Long): kotlinx.datetime.LocalDateTime {
        return Instant.fromEpochMilliseconds(epochMillis)
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

    private fun createStatusFilters(status: Status): List<NotificationUsageListScreenUiState.Filter> {
        return when (mode) {
            Mode.AddFromNotification -> listOf(Status.NotAdded, Status.Added)
            Mode.NotificationList -> listOf(Status.All, Status.Added, Status.NotAdded)
        }.map { filter ->
            NotificationUsageListScreenUiState.Filter(
                label = filter.label,
                selected = filter == status,
                listener = object : NotificationUsageListScreenUiState.FilterListener {
                    override fun onClick() {
                        viewModelStateFlow.update { it.copy(status = filter) }
                    }
                },
            )
        }
    }

    private fun createSearchListener(mode: Mode): NotificationUsageListScreenUiState.SearchListener? {
        if (mode != Mode.NotificationList) return null
        return object : NotificationUsageListScreenUiState.SearchListener {
            override fun onSearchQueryChange(query: String) {
                viewModelStateFlow.update { it.copy(searchQuery = query) }
            }
        }
    }

    private fun filterByQuery(
        items: List<ViewModelState.ItemSource>,
        query: String,
    ): List<ViewModelState.ItemSource> {
        if (query.isBlank()) return items
        val q = query.lowercase()
        return items.filter { source ->
            when (source) {
                is ViewModelState.ItemSource.Matched -> {
                    source.record.record.packageName.lowercase().contains(q) ||
                        source.record.record.text.lowercase().contains(q)
                }

                is ViewModelState.ItemSource.Raw -> {
                    source.record.packageName.lowercase().contains(q) ||
                        source.record.text.lowercase().contains(q)
                }
            }
        }
    }

    private fun modeTitle(mode: Mode): String {
        return when (mode) {
            Mode.AddFromNotification -> "通知から追加"
            Mode.NotificationList -> "通知一覧"
        }
    }

    private fun initialStatus(mode: Mode): Status {
        return when (mode) {
            Mode.AddFromNotification -> Status.NotAdded
            Mode.NotificationList -> Status.All
        }
    }

    private fun emptyText(mode: Mode, status: Status): String {
        return when (mode) {
            Mode.AddFromNotification -> status.addFromNotificationEmptyText
            Mode.NotificationList -> status.notificationListEmptyText
        }
    }

    public enum class Mode {
        AddFromNotification,
        NotificationList,
    }

    public interface Event {
        public fun navigate(structure: ScreenStructure)

        public fun navigateToHome()

        public fun copyToClipboard(text: String)

        public fun showToast(text: String)
    }

    private data class ViewModelState(
        val status: Status,
        val searchQuery: String = "",
        val accessState: NotificationAccessState,
        val itemsLoadingState: ItemsLoadingState,
    ) {
        sealed interface ItemsLoadingState {
            data object Loading : ItemsLoadingState

            data class Loaded(val items: List<ItemSource>) : ItemsLoadingState
        }

        sealed interface ItemSource {
            data class Matched(val record: NotificationUsageMatchedRecord) : ItemSource

            data class Raw(val record: NotificationUsageRecord) : ItemSource
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
