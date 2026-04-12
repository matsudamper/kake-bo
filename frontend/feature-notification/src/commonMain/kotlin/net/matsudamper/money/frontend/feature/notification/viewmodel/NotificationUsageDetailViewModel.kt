package net.matsudamper.money.frontend.feature.notification.viewmodel

import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageDetail
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageMatchedRecord
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRepository
import net.matsudamper.money.frontend.common.base.runCatchingWithoutCancel
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.feature.notification.ui.NotificationUsageDetailScreenUiState
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.MoneyUsageScreenQuery
import net.matsudamper.money.frontend.graphql.fragment.MoneyUsageScreenMoneyUsage

public class NotificationUsageDetailViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val notificationUsageKey: String,
    private val repository: NotificationUsageRepository,
    private val graphqlClient: GraphqlClient,
) : CommonViewModel(scopedObjectFeature) {
    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiStateFlow: StateFlow<NotificationUsageDetailScreenUiState> = MutableStateFlow(
        NotificationUsageDetailScreenUiState(
            loadingState = NotificationUsageDetailScreenUiState.LoadingState.Loading,
            event = object : NotificationUsageDetailScreenUiState.Event {
                override fun onClickBack() {
                    viewModelScope.launch {
                        eventSender.send { it.navigateBack() }
                    }
                }

                override fun onClickTitle() {
                    viewModelScope.launch {
                        eventSender.send { it.navigateToHome() }
                    }
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        loadingState = viewModelState.toLoadingState(),
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.notificationDetailFlow(notificationUsageKey).collectLatest { detail ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        detailState = if (detail == null) {
                            DetailState.NotFound
                        } else {
                            DetailState.Loaded(detail)
                        },
                    )
                }
                fetchLinkedUsage(detail?.record?.moneyUsageId)
            }
        }
    }.asStateFlow()

    private fun ViewModelState.toLoadingState(): NotificationUsageDetailScreenUiState.LoadingState {
        val detail = when (val detailState = detailState) {
            DetailState.Loading -> return NotificationUsageDetailScreenUiState.LoadingState.Loading
            DetailState.NotFound -> return NotificationUsageDetailScreenUiState.LoadingState.NotFound
            is DetailState.Loaded -> detailState.detail
        }

        return NotificationUsageDetailScreenUiState.LoadingState.Loaded(
            notification = detail.record.toNotificationUiState(),
            filter = detail.matched.toFilterUiState(),
            draft = detail.matched?.toDraftUiState(),
            canRegister = detail.record.isAdded.not(),
            linkedUsage = linkedUsageState.toUiState(),
            event = object : NotificationUsageDetailScreenUiState.LoadedEvent {
                override fun onClickRegister() {
                    viewModelScope.launch {
                        eventSender.send { it.navigate(detail.toAddMoneyUsageScreen()) }
                    }
                }
            },
        )
    }

    private suspend fun fetchLinkedUsage(moneyUsageId: MoneyUsageId?) {
        if (moneyUsageId == null) {
            val detail = viewModelStateFlow.value.detailState as? DetailState.Loaded
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    linkedUsageState = if (detail?.detail?.record?.isAdded == true) {
                        LinkedUsageState.MissingUsageId
                    } else {
                        LinkedUsageState.None
                    },
                )
            }
            return
        }

        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(linkedUsageState = LinkedUsageState.Loading(moneyUsageId))
        }
        val result = runCatchingWithoutCancel {
            graphqlClient.apolloClient
                .query(MoneyUsageScreenQuery(id = moneyUsageId))
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }.getOrNull()

        val moneyUsage = result?.data?.user?.moneyUsage?.moneyUsageScreenMoneyUsage
        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                linkedUsageState = if (result == null || result.hasErrors() || moneyUsage == null) {
                    LinkedUsageState.Error(moneyUsageId)
                } else {
                    LinkedUsageState.Loaded(moneyUsageId, moneyUsage)
                },
            )
        }
    }

    private fun NotificationUsageRecord.toNotificationUiState(): NotificationUsageDetailScreenUiState.Notification {
        return NotificationUsageDetailScreenUiState.Notification(
            packageName = packageName,
            status = if (isAdded) "追加済み" else "未追加",
            postedAt = Formatter.formatDateTime(postedAtEpochMillis.toLocalDateTime()),
            receivedAt = Formatter.formatDateTime(receivedAtEpochMillis.toLocalDateTime()),
            text = text.ifBlank { "(テキストなし)" },
        )
    }

    private fun NotificationUsageMatchedRecord?.toFilterUiState(): NotificationUsageDetailScreenUiState.Filter {
        if (this == null) {
            return NotificationUsageDetailScreenUiState.Filter.NotMatched
        }
        return NotificationUsageDetailScreenUiState.Filter.Matched(
            title = filterDefinition.title,
            description = filterDefinition.description,
        )
    }

    private fun NotificationUsageMatchedRecord.toDraftUiState(): NotificationUsageDetailScreenUiState.Draft {
        return NotificationUsageDetailScreenUiState.Draft(
            title = draft.title ?: record.packageName,
            description = draft.description ?: record.text,
            amount = draft.amount?.let { "${Formatter.formatMoney(it)}円" }.orEmpty(),
            dateTime = draft.dateTime?.let { Formatter.formatDateTime(it) }.orEmpty(),
            subCategory = draft.subCategoryId?.id?.toString().orEmpty(),
        )
    }

    private fun LinkedUsageState.toUiState(): NotificationUsageDetailScreenUiState.LinkedUsageState {
        return when (this) {
            LinkedUsageState.None -> NotificationUsageDetailScreenUiState.LinkedUsageState.None
            is LinkedUsageState.Loading -> NotificationUsageDetailScreenUiState.LinkedUsageState.Loading
            LinkedUsageState.MissingUsageId -> NotificationUsageDetailScreenUiState.LinkedUsageState.MissingUsageId
            is LinkedUsageState.Error -> NotificationUsageDetailScreenUiState.LinkedUsageState.Error
            is LinkedUsageState.Loaded -> NotificationUsageDetailScreenUiState.LinkedUsageState.Loaded(
                usage = moneyUsage.toLinkedUsageUiState(moneyUsageId),
            )
        }
    }

    private fun MoneyUsageScreenMoneyUsage.toLinkedUsageUiState(
        moneyUsageId: MoneyUsageId,
    ): NotificationUsageDetailScreenUiState.LinkedUsage {
        return NotificationUsageDetailScreenUiState.LinkedUsage(
            title = title,
            category = run {
                val subCategory = moneyUsageSubCategory ?: return@run "未指定"
                "${subCategory.category.name} / ${subCategory.name}"
            },
            amount = "${Formatter.formatMoney(amount)}円",
            dateTime = Formatter.formatDateTime(date),
            event = object : NotificationUsageDetailScreenUiState.LinkedUsageEvent {
                override fun onClick() {
                    viewModelScope.launch {
                        eventSender.send { it.navigate(ScreenStructure.MoneyUsage(moneyUsageId)) }
                    }
                }
            },
        )
    }

    private fun NotificationUsageDetail.toAddMoneyUsageScreen(): ScreenStructure.AddMoneyUsage {
        val draft = matched?.draft
        val description = buildString {
            val draftDesc = draft?.description
            if (!draftDesc.isNullOrBlank()) {
                append(draftDesc)
                append("\n")
            }
            append(record.text)
        }
        return ScreenStructure.AddMoneyUsage(
            title = draft?.title,
            description = description,
            price = draft?.amount?.toFloat(),
            date = draft?.dateTime,
            subCategoryId = draft?.subCategoryId?.id?.toString(),
            notificationUsageKey = record.notificationKey,
        )
    }

    private fun Long.toLocalDateTime(): kotlinx.datetime.LocalDateTime {
        return Instant.fromEpochMilliseconds(this)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    }

    private sealed interface DetailState {
        data object Loading : DetailState

        data object NotFound : DetailState

        data class Loaded(val detail: NotificationUsageDetail) : DetailState
    }

    private sealed interface LinkedUsageState {
        data object None : LinkedUsageState

        data class Loading(val moneyUsageId: MoneyUsageId) : LinkedUsageState

        data object MissingUsageId : LinkedUsageState

        data class Error(val moneyUsageId: MoneyUsageId) : LinkedUsageState

        data class Loaded(
            val moneyUsageId: MoneyUsageId,
            val moneyUsage: MoneyUsageScreenMoneyUsage,
        ) : LinkedUsageState
    }

    private data class ViewModelState(
        val detailState: DetailState = DetailState.Loading,
        val linkedUsageState: LinkedUsageState = LinkedUsageState.None,
    )

    public interface Event {
        public fun navigate(structure: ScreenStructure)

        public fun navigateBack()

        public fun navigateToHome()
    }
}
