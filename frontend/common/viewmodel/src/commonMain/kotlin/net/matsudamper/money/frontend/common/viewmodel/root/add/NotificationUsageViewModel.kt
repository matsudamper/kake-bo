package net.matsudamper.money.frontend.common.viewmodel.root.add

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
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
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val addedFilterStateFlow = MutableStateFlow(AddedFilter.NotAdded)

    public enum class Mode {
        Matched,
        Unmatched,
    }

    public val uiStateFlow: StateFlow<NotificationUsageListScreenUiState> = MutableStateFlow(
        NotificationUsageListScreenUiState(
            title = mode.title,
            items = emptyList<NotificationUsageListScreenUiState.Item>().toImmutableList(),
            filters = addedFilterStateFlow.value.toUiState().toImmutableList(),
            emptyText = mode.emptyText,
            accessSection = null,
            topBarActions = mode.topBarActions(navController).toImmutableList(),
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
                repository.matchedNotificationsFlow(),
                repository.unmatchedNotificationsFlow(),
                addedFilterStateFlow,
            ) { accessState, matchedRecords, unmatchedRecords, addedFilter ->
                NotificationUsageUiStateSource(
                    accessState = accessState,
                    matchedRecords = matchedRecords,
                    unmatchedRecords = unmatchedRecords,
                    addedFilter = addedFilter,
                )
            }.collect { source ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        items = when (mode) {
                            Mode.Matched ->
                                source.matchedRecords
                                    .filter { source.addedFilter.matches(it.record.isAdded) }
                                    .map { it.toUiItem(navController) }
                            Mode.Unmatched ->
                                source.unmatchedRecords
                                    .filter { source.addedFilter.matches(it.isAdded) }
                                    .map { it.toUiItem() }
                        }.toImmutableList(),
                        filters = source.addedFilter.toUiState().toImmutableList(),
                        accessSection = source.accessState.toAccessSection(),
                    )
                }
            }
        }
    }.asStateFlow()

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

    private fun NotificationUsageMatchedRecord.toUiItem(
        navController: ScreenNavController,
    ): NotificationUsageListScreenUiState.Item {
        return NotificationUsageListScreenUiState.Item(
            title = record.packageName,
            statusLabel = record.addedLabel,
            description = record.text.ifBlank { "(テキストなし)" },
            onClick = {
                navController.navigate(
                    ScreenStructure.AddMoneyUsage(
                        title = draft.title,
                        description = draft.description,
                        price = draft.amount?.toFloat(),
                        date = draft.dateTime,
                        subCategoryId = draft.subCategoryId?.id?.toString(),
                        notificationUsageKey = record.notificationKey,
                    ),
                )
            },
        )
    }

    private fun NotificationUsageRecord.toUiItem(): NotificationUsageListScreenUiState.Item {
        return NotificationUsageListScreenUiState.Item(
            title = packageName,
            statusLabel = addedLabel,
            description = text.ifBlank { "(テキストなし)" },
        )
    }

    private val NotificationUsageRecord.addedLabel: String
        get() = if (isAdded) "追加済み" else "未追加"

    private val Mode.title: String
        get() = when (this) {
            Mode.Matched -> "通知から追加"
            Mode.Unmatched -> "通知の確認"
        }

    private val Mode.emptyText: String
        get() = when (this) {
            Mode.Matched -> "一致する通知はありません。"
            Mode.Unmatched -> "未一致の通知はありません。"
        }

    private fun Mode.topBarActions(navController: ScreenNavController): List<NotificationUsageListScreenUiState.TopBarAction> {
        return when (this) {
            Mode.Matched -> listOf(
                NotificationUsageListScreenUiState.TopBarAction(
                    label = "条件",
                    onClick = {
                        navController.navigate(ScreenStructure.Root.Add.NotificationUsageFilters)
                    },
                ),
                NotificationUsageListScreenUiState.TopBarAction(
                    label = "？",
                    onClick = {
                        navController.navigate(ScreenStructure.Root.Add.NotificationUsageDebug)
                    },
                ),
            )

            Mode.Unmatched -> emptyList()
        }
    }

    private fun AddedFilter.toUiState(): List<NotificationUsageListScreenUiState.Filter> {
        return AddedFilter.entries.map { filter ->
            NotificationUsageListScreenUiState.Filter(
                label = filter.label,
                selected = filter == this,
                onClick = {
                    addedFilterStateFlow.value = filter
                },
            )
        }
    }

    private data class NotificationUsageUiStateSource(
        val accessState: NotificationAccessState,
        val matchedRecords: List<NotificationUsageMatchedRecord>,
        val unmatchedRecords: List<NotificationUsageRecord>,
        val addedFilter: AddedFilter,
    )

    private enum class AddedFilter(
        val label: String,
    ) {
        All("全て"),
        NotAdded("未追加"),
        Added("追加済み"),
        ;

        fun matches(isAdded: Boolean): Boolean {
            return when (this) {
                All -> true
                NotAdded -> !isAdded
                Added -> isAdded
            }
        }
    }
}
