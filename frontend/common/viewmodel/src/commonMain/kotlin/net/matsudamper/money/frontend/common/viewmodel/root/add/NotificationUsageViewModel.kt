package net.matsudamper.money.frontend.common.viewmodel.root.add

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.base.notification.NotificationAccessState
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageAccessGateway
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageMatchedRecord
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRepository
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.root.add.NotificationUsageListScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel

public class NotificationUsageViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val mode: Mode,
    private val repository: NotificationUsageRepository,
    private val accessGateway: NotificationUsageAccessGateway,
    private val navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val statusStateFlow = MutableStateFlow(mode.initialStatus)

    public enum class Mode {
        AddFromNotification,
        NotificationList,
    }

    public val uiStateFlow: StateFlow<NotificationUsageListScreenUiState> = MutableStateFlow(
        NotificationUsageListScreenUiState(
            title = mode.title,
            items = emptyList<NotificationUsageListScreenUiState.Item>().toImmutableList(),
            filters = statusStateFlow.value.toUiState().toImmutableList(),
            emptyText = mode.emptyText(statusStateFlow.value),
            accessSection = null,
            topBarActions = mode.topBarActions().toImmutableList(),
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navController.navigateToHome()
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            combine(
                accessGateway.accessStateFlow(),
                statusStateFlow,
                itemsFlow(),
            ) { accessState, status, items ->
                NotificationUsageUiStateSource(
                    accessState = accessState,
                    status = status,
                    items = items,
                )
            }.collect { source ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        items = source.items.map { it.toUiItem() }.toImmutableList(),
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
                        navController.navigate(ScreenStructure.Root.Add.NotificationUsageFilters)
                    },
                ),
                NotificationUsageListScreenUiState.TopBarAction(
                    label = "一覧",
                    onClick = {
                        navController.navigate(ScreenStructure.Root.Add.NotificationUsageDebug)
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

            NotificationAccessState.Unsupported -> NotificationUsageListScreenUiState.AccessSection(
                title = "この機能は利用できません",
                description = "通知から追加は Android でのみ利用できます。",
            )
        }
    }

    private fun ItemSource.toUiItem(): NotificationUsageListScreenUiState.Item {
        return when (this) {
            is ItemSource.Matched -> record.toUiItem(
                title = record.record.packageName,
                statusLabel = "未追加",
                description = record.record.text.ifBlank { "(テキストなし)" },
            )

            is ItemSource.Raw -> record.toUiItem(
                title = record.packageName,
                statusLabel = record.addedLabel,
                description = record.text.ifBlank { "(テキストなし)" },
            )
        }
    }

    private fun NotificationUsageMatchedRecord.toUiItem(
        title: String,
        statusLabel: String,
        description: String,
    ): NotificationUsageListScreenUiState.Item {
        return NotificationUsageListScreenUiState.Item(
            title = title,
            statusLabel = statusLabel,
            description = description,
            onClick = {
                navController.navigate(
                    ScreenStructure.NotificationUsageDetail(
                        notificationUsageKey = record.notificationKey,
                    ),
                )
            },
        )
    }

    private fun NotificationUsageRecord.toUiItem(
        title: String,
        statusLabel: String,
        description: String,
    ): NotificationUsageListScreenUiState.Item {
        return NotificationUsageListScreenUiState.Item(
            title = title,
            statusLabel = statusLabel,
            description = description,
            onClick = {
                navController.navigate(
                    ScreenStructure.NotificationUsageDetail(
                        notificationUsageKey = notificationKey,
                    ),
                )
            },
        )
    }

    private fun Status.toUiState(): List<NotificationUsageListScreenUiState.Filter> {
        return mode.statuses.map { filter ->
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
        val items: List<ItemSource>,
    )

    private sealed interface ItemSource {
        data class Matched(val record: NotificationUsageMatchedRecord) : ItemSource

        data class Raw(val record: NotificationUsageRecord) : ItemSource
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

    private val Mode.statuses: List<Status>
        get() = when (this) {
            Mode.AddFromNotification -> listOf(Status.NotAdded, Status.Added)
            Mode.NotificationList -> listOf(Status.All, Status.Added, Status.NotAdded)
        }

    private val NotificationUsageRecord.addedLabel: String
        get() = if (isAdded) "追加済み" else "未追加"

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
