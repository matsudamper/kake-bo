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
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import net.matsudamper.money.categoryfilter.CategoryFilter
import net.matsudamper.money.categoryfilter.CategoryFilterCondition
import net.matsudamper.money.categoryfilter.CategoryFilterConditionType
import net.matsudamper.money.categoryfilter.CategoryFilterDataSourceType
import net.matsudamper.money.categoryfilter.CategoryFilterOperator
import net.matsudamper.money.categoryfilter.evaluateCategoryFilters
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageDetail
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageKey
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
import net.matsudamper.money.frontend.graphql.NotificationUsageCategoryFiltersQuery
import net.matsudamper.money.frontend.graphql.fragment.MoneyUsageScreenMoneyUsage
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterConditionType
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterDataSourceType
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFiltersQuery
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFiltersSortType
import net.matsudamper.money.frontend.graphql.type.ImportedMailFilterCategoryConditionOperator

public class NotificationUsageDetailViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val notificationUsageKey: NotificationUsageKey,
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
                        loadingState = createLoadingState(viewModelState),
                    )
                }
            }
        }
    }.asStateFlow()

    init {
        viewModelScope.launch {
            repository.notificationDetailFlow(notificationUsageKey).collectLatest { detail ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        detailState = if (detail == null) {
                            DetailState.NotFound
                        } else {
                            DetailState.Loaded(detail)
                        },
                        matchedSubCategory = null,
                    )
                }
                fetchLinkedUsage(detail?.record?.moneyUsageId)
                fetchMatchedSubCategory(detail?.matched)
            }
        }
    }

    private suspend fun fetchMatchedSubCategory(matched: NotificationUsageMatchedRecord?) {
        if (matched == null) return

        val response = runCatchingWithoutCancel {
            graphqlClient.apolloClient
                .query(
                    NotificationUsageCategoryFiltersQuery(
                        query = ImportedMailCategoryFiltersQuery(
                            size = Optional.present(1000),
                            isAsc = true,
                            sortType = Optional.present(ImportedMailCategoryFiltersSortType.ORDER_NUMBER),
                        ),
                    ),
                )
                .execute()
        }.getOrNull() ?: return

        val nodes = response.data?.user?.importedMailCategoryFilters?.nodes.orEmpty()
        val filters = nodes.map { node ->
            CategoryFilter(
                orderNumber = node.orderNumber,
                operator = node.operator.toShared(),
                subCategoryId = node.subCategory?.id,
                conditions = node.conditions.orEmpty().map { c ->
                    CategoryFilterCondition(
                        text = c.text,
                        dataSourceType = c.dataSourceType.toShared(),
                        conditionType = c.conditionType.toShared(),
                    )
                },
            )
        }

        val subCategoryId = evaluateCategoryFilters(filters) { dataSourceType ->
            when (dataSourceType) {
                CategoryFilterDataSourceType.Title -> matched.draft.title
                CategoryFilterDataSourceType.ServiceName -> matched.filterDefinition.title
                else -> null
            }
        }

        if (subCategoryId == null) return

        val matchedNode = nodes.firstOrNull { it.subCategory?.id == subCategoryId }
        val subCategory = matchedNode?.subCategory ?: return

        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                matchedSubCategory = MatchedSubCategory(
                    subCategoryId = subCategoryId,
                    displayName = "${subCategory.category.name} / ${subCategory.name}",
                ),
            )
        }
    }

    private fun createLoadingState(viewModelState: ViewModelState): NotificationUsageDetailScreenUiState.LoadingState {
        val detail = when (val detailState = viewModelState.detailState) {
            DetailState.Loading -> return NotificationUsageDetailScreenUiState.LoadingState.Loading
            DetailState.NotFound -> return NotificationUsageDetailScreenUiState.LoadingState.NotFound
            is DetailState.Loaded -> detailState.detail
        }

        val matched = detail.matched
        return NotificationUsageDetailScreenUiState.LoadingState.Loaded(
            notification = createNotificationUiState(detail.record),
            filter = createFilterUiState(matched),
            draft = if (matched != null) createDraftUiState(matched, viewModelState.matchedSubCategory) else null,
            linkedUsage = createLinkedUsageUiState(viewModelState.linkedUsageState),
            metadataDialog = if (viewModelState.showMetadataDialog) {
                NotificationUsageDetailScreenUiState.MetadataDialog(
                    text = detail.record.notificationMetadata,
                    event = object : NotificationUsageDetailScreenUiState.MetadataDialogEvent {
                        override fun onDismiss() {
                            viewModelStateFlow.update { it.copy(showMetadataDialog = false) }
                        }
                    },
                )
            } else {
                null
            },
            event = object : NotificationUsageDetailScreenUiState.LoadedEvent {
                override fun onClickRegister() {
                    viewModelScope.launch {
                        eventSender.send { it.navigate(createAddMoneyUsageScreen(detail, viewModelStateFlow.value.matchedSubCategory)) }
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

    private fun createNotificationUiState(record: NotificationUsageRecord): NotificationUsageDetailScreenUiState.Notification {
        return NotificationUsageDetailScreenUiState.Notification(
            packageName = record.packageName,
            status = if (record.isAdded) "追加済み" else "未追加",
            postedAt = Formatter.formatDateTime(toLocalDateTime(record.postedAtEpochMillis)),
            receivedAt = Formatter.formatDateTime(toLocalDateTime(record.receivedAtEpochMillis)),
            text = record.text.ifBlank { "(テキストなし)" },
            metadata = record.notificationMetadata,
            event = object : NotificationUsageDetailScreenUiState.NotificationEvent {
                override fun onClickMetadata() {
                    viewModelStateFlow.update { it.copy(showMetadataDialog = true) }
                }
            },
        )
    }

    private fun createFilterUiState(matched: NotificationUsageMatchedRecord?): NotificationUsageDetailScreenUiState.Filter {
        if (matched == null) {
            return NotificationUsageDetailScreenUiState.Filter.NotMatched
        }
        return NotificationUsageDetailScreenUiState.Filter.Matched(
            title = matched.filterDefinition.title,
            description = matched.filterDefinition.description,
        )
    }

    private fun createDraftUiState(
        matched: NotificationUsageMatchedRecord,
        matchedSubCategory: MatchedSubCategory?,
    ): NotificationUsageDetailScreenUiState.Draft {
        return NotificationUsageDetailScreenUiState.Draft(
            title = matched.draft.title,
            description = matched.draft.description,
            amount = matched.draft.amount?.let { "${Formatter.formatMoney(it)}円" }.orEmpty(),
            dateTime = matched.draft.dateTime.let { Formatter.formatDateTime(it) },
            subCategory = matchedSubCategory?.displayName.orEmpty(),
        )
    }

    private fun createLinkedUsageUiState(linkedUsageState: LinkedUsageState): NotificationUsageDetailScreenUiState.LinkedUsageState {
        return when (linkedUsageState) {
            LinkedUsageState.None -> NotificationUsageDetailScreenUiState.LinkedUsageState.None
            is LinkedUsageState.Loading -> NotificationUsageDetailScreenUiState.LinkedUsageState.Loading
            LinkedUsageState.MissingUsageId -> NotificationUsageDetailScreenUiState.LinkedUsageState.MissingUsageId
            is LinkedUsageState.Error -> NotificationUsageDetailScreenUiState.LinkedUsageState.Error
            is LinkedUsageState.Loaded -> NotificationUsageDetailScreenUiState.LinkedUsageState.Loaded(
                usage = createLinkedUsageItemUiState(linkedUsageState.moneyUsageId, linkedUsageState.moneyUsage),
            )
        }
    }

    private fun createLinkedUsageItemUiState(
        moneyUsageId: MoneyUsageId,
        moneyUsage: MoneyUsageScreenMoneyUsage,
    ): NotificationUsageDetailScreenUiState.LinkedUsage {
        return NotificationUsageDetailScreenUiState.LinkedUsage(
            title = moneyUsage.title,
            category = run {
                val subCategory = moneyUsage.moneyUsageSubCategory ?: return@run "未指定"
                "${subCategory.category.name} / ${subCategory.name}"
            },
            amount = "${Formatter.formatMoney(moneyUsage.amount)}円",
            dateTime = Formatter.formatDateTime(moneyUsage.date),
            event = object : NotificationUsageDetailScreenUiState.LinkedUsageEvent {
                override fun onClick() {
                    viewModelScope.launch {
                        eventSender.send { it.navigate(ScreenStructure.MoneyUsage(moneyUsageId)) }
                    }
                }
            },
        )
    }

    private fun createAddMoneyUsageScreen(
        detail: NotificationUsageDetail,
        matchedSubCategory: MatchedSubCategory?,
    ): ScreenStructure.AddMoneyUsage {
        val draft = detail.matched?.draft
        val description = draft?.description ?: detail.record.text
        return ScreenStructure.AddMoneyUsage(
            title = draft?.title,
            description = description,
            price = draft?.amount?.toFloat(),
            date = draft?.dateTime,
            subCategoryId = matchedSubCategory?.subCategoryId?.id?.toString(),
            notificationUsageKey = detail.record.notificationKey,
        )
    }

    private fun toLocalDateTime(epochMillis: Long): kotlinx.datetime.LocalDateTime {
        return Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    }

    private fun ImportedMailFilterCategoryConditionOperator.toShared(): CategoryFilterOperator {
        return when (this) {
            ImportedMailFilterCategoryConditionOperator.AND -> CategoryFilterOperator.AND
            ImportedMailFilterCategoryConditionOperator.OR -> CategoryFilterOperator.OR
            ImportedMailFilterCategoryConditionOperator.UNKNOWN__ -> CategoryFilterOperator.AND
        }
    }

    private fun ImportedMailCategoryFilterDataSourceType.toShared(): CategoryFilterDataSourceType {
        return when (this) {
            ImportedMailCategoryFilterDataSourceType.MailTitle -> CategoryFilterDataSourceType.MailTitle
            ImportedMailCategoryFilterDataSourceType.MailFrom -> CategoryFilterDataSourceType.MailFrom
            ImportedMailCategoryFilterDataSourceType.MailHtml -> CategoryFilterDataSourceType.MailHtml
            ImportedMailCategoryFilterDataSourceType.MailPlain -> CategoryFilterDataSourceType.MailPlain
            ImportedMailCategoryFilterDataSourceType.Title -> CategoryFilterDataSourceType.Title
            ImportedMailCategoryFilterDataSourceType.ServiceName -> CategoryFilterDataSourceType.ServiceName
            ImportedMailCategoryFilterDataSourceType.UNKNOWN__ -> CategoryFilterDataSourceType.Title
        }
    }

    private fun ImportedMailCategoryFilterConditionType.toShared(): CategoryFilterConditionType {
        return when (this) {
            ImportedMailCategoryFilterConditionType.Include -> CategoryFilterConditionType.Include
            ImportedMailCategoryFilterConditionType.NotInclude -> CategoryFilterConditionType.NotInclude
            ImportedMailCategoryFilterConditionType.Equal -> CategoryFilterConditionType.Equal
            ImportedMailCategoryFilterConditionType.NotEqual -> CategoryFilterConditionType.NotEqual
            ImportedMailCategoryFilterConditionType.UNKNOWN__ -> CategoryFilterConditionType.Include
        }
    }

    public interface Event {
        public fun navigate(structure: ScreenStructure)

        public fun navigateBack()

        public fun navigateToHome()
    }

    private data class MatchedSubCategory(
        val subCategoryId: MoneyUsageSubCategoryId,
        val displayName: String,
    )

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
        val showMetadataDialog: Boolean = false,
        val matchedSubCategory: MatchedSubCategory? = null,
    )
}
