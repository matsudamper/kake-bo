package net.matsudamper.money.frontend.common.viewmodel.moneyusage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.screen.moneyusage.MoneyUsageScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.layout.CategorySelectDialogViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.MoneyUsageScreenQuery
import net.matsudamper.money.frontend.graphql.fragment.MoneyUsageScreenMoneyUsage
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState

public class MoneyUsageScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val moneyUsageId: MoneyUsageId,
    private val api: MoneyUsageScreenViewModelApi,
    apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val categorySelectDialogViewModel = object {
        private val event: CategorySelectDialogViewModel.Event = object : CategorySelectDialogViewModel.Event {
            override fun selected(result: CategorySelectDialogViewModel.SelectedResult) {
                coroutineScope.launch {
                    val isSuccess = api.updateUsage(
                        id = moneyUsageId,
                        subCategoryId = result.subCategoryId,
                    )
                    if (isSuccess) {
                        viewModel.dismissDialog()
                    } else {
                        // TODO
                    }
                }
            }
        }
        val viewModel = CategorySelectDialogViewModel(
            coroutineScope = coroutineScope,
            event = event,
        )
    }.viewModel
    public val uiStateFlow: StateFlow<MoneyUsageScreenUiState> = MutableStateFlow(
        MoneyUsageScreenUiState(
            event = object : MoneyUsageScreenUiState.Event {
                override fun onViewInitialized() {
                    coroutineScope.launch {
                        apolloCollector.fetch(this)
                    }
                }

                override fun onClickRetry() {
                    coroutineScope.launch {
                        apolloCollector.fetch(this)
                    }
                }

                override fun onClickBack() {
                    coroutineScope.launch {
                        eventSender.send {
                            it.navigateBack()
                        }
                    }
                }
            },
            loadingState = MoneyUsageScreenUiState.LoadingState.Loading,
            confirmDialog = null,
            textInputDialog = null,
            calendarDialog = null,
            categorySelectDialog = null,
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val loadingState = run loadingState@{
                    when (val state = viewModelState.apolloResponseState) {
                        is ApolloResponseState.Failure -> {
                            MoneyUsageScreenUiState.LoadingState.Error
                        }

                        is ApolloResponseState.Loading -> {
                            MoneyUsageScreenUiState.LoadingState.Loading
                        }

                        is ApolloResponseState.Success -> {
                            val moneyUsage = state.value.data?.user?.moneyUsage?.moneyUsageScreenMoneyUsage
                                ?: return@loadingState MoneyUsageScreenUiState.LoadingState.Error

                            MoneyUsageScreenUiState.LoadingState.Loaded(
                                moneyUsage = MoneyUsageScreenUiState.MoneyUsage(
                                    title = moneyUsage.title,
                                    amount = "${Formatter.formatMoney(moneyUsage.amount)}円",
                                    description = moneyUsage.description,
                                    dateTime = moneyUsage.date.toString(),
                                    category = run category@{
                                        val subCategory = moneyUsage.moneyUsageSubCategory ?: return@category "未指定"
                                        val category = subCategory.category
                                        "${subCategory.name} / ${category.name}"
                                    },
                                    event = createMoneyUsageEvent(item = moneyUsage),
                                ),
                                linkedMails = moneyUsage.linkedMail.orEmpty().map { mail ->
                                    MoneyUsageScreenUiState.MailItem(
                                        subject = mail.subject,
                                        from = mail.from,
                                        date = mail.dateTime.toString(),
                                        event = object : MoneyUsageScreenUiState.MailItemEvent {
                                            override fun onClick() {
                                                coroutineScope.launch {
                                                    eventSender.send {
                                                        it.navigate(ScreenStructure.ImportedMail(id = mail.id))
                                                    }
                                                }
                                            }
                                        },
                                    )
                                }.toImmutableList(),
                                event = createLoadedEvent(),
                            )
                        }
                    }
                }

                uiStateFlow.update { uiState ->
                    uiState.copy(
                        loadingState = loadingState,
                        confirmDialog = viewModelState.confirmDialog,
                        textInputDialog = viewModelState.textInputDialog,
                        calendarDialog = viewModelState.calendarDialog,
                        categorySelectDialog = viewModelState.categorySelectDialog,
                    )
                }
            }
        }
    }.asStateFlow()

    private fun createLoadedEvent(): MoneyUsageScreenUiState.LoadedEvent {
        return object : MoneyUsageScreenUiState.LoadedEvent {
            override fun onClickDelete() {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        confirmDialog = MoneyUsageScreenUiState.ConfirmDialog(
                            title = "削除しますか？",
                            description = null,
                            onConfirm = {
                                coroutineScope.launch {
                                    val isSuccess = api.deleteUsage(
                                        id = moneyUsageId,
                                    )
                                    if (isSuccess) {
                                        dismissConfirmDialog()
                                        eventSender.send {
                                            it.navigateBack()
                                        }
                                    } else {
                                        // TODO
                                    }
                                }
                            },
                            onDismiss = { dismissConfirmDialog() },
                        ),
                    )
                }
            }
        }
    }

    private fun createMoneyUsageEvent(item: MoneyUsageScreenMoneyUsage): MoneyUsageScreenUiState.MoneyUsageEvent {
        return object : MoneyUsageScreenUiState.MoneyUsageEvent {
            override fun onClickTitleChange() {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        textInputDialog = MoneyUsageScreenUiState.TextInputDialog(
                            isMultiline = false,
                            title = "タイトル",
                            onComplete = { text ->
                                coroutineScope.launch {
                                    val isSuccess = api.updateUsage(
                                        id = moneyUsageId,
                                        title = text,
                                    )
                                    if (isSuccess) {
                                        dismissTextInputDialog()
                                    } else {
                                        // TODO
                                    }
                                }
                            },
                            default = item.title,
                            onCancel = { dismissTextInputDialog() },
                        ),
                    )
                }
            }

            override fun onClickCategoryChange() {
                categorySelectDialogViewModel.showDialog(
                    categoryId = item.moneyUsageSubCategory?.category?.id,
                    categoryName = item.moneyUsageSubCategory?.category?.name,
                    subCategoryId = item.moneyUsageSubCategory?.id,
                    subCategoryName = item.moneyUsageSubCategory?.name,
                )
            }

            override fun onClickDateChange() {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        calendarDialog = MoneyUsageScreenUiState.CalendarDialog(
                            onSelectedDate = { date ->
                                coroutineScope.launch {
                                    val isSuccess = api.updateUsage(
                                        id = moneyUsageId,
                                        date = LocalDateTime(date, item.date.time),
                                    )
                                    if (isSuccess) {
                                        dismissCalendarDialog()
                                    } else {
                                        // TODO
                                    }
                                }
                            },
                            dismissRequest = { dismissCalendarDialog() },
                            date = item.date.date,
                        ),
                    )
                }
            }

            override fun onClickAmountChange() {
            }

            override fun onClickDescription() {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        textInputDialog = MoneyUsageScreenUiState.TextInputDialog(
                            isMultiline = true,
                            title = "説明",
                            onComplete = { text ->
                                coroutineScope.launch {
                                    val isSuccess = api.updateUsage(
                                        id = moneyUsageId,
                                        description = text,
                                    )
                                    if (isSuccess) {
                                        dismissTextInputDialog()
                                    } else {
                                        // TODO
                                    }
                                }
                            },
                            default = item.description,
                            onCancel = { dismissTextInputDialog() },
                        ),
                    )
                }
            }
        }
    }

    private val apolloCollector = ApolloResponseCollector.create(
        apolloClient = apolloClient,
        query = MoneyUsageScreenQuery(
            id = moneyUsageId,
        ),
    )

    init {
        coroutineScope.launch {
            apolloCollector.flow.collectLatest { apolloResponseState ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        apolloResponseState = apolloResponseState,
                    )
                }
            }
        }
        coroutineScope.launch {
            categorySelectDialogViewModel.getUiStateFlow().collectLatest { categorySelectDialogUiState ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        categorySelectDialog = categorySelectDialogUiState,
                    )
                }
            }
        }
    }

    private fun dismissConfirmDialog() {
        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                confirmDialog = null,
            )
        }
    }

    private fun dismissCalendarDialog() {
        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                calendarDialog = null,
            )
        }
    }

    private fun dismissTextInputDialog() {
        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                textInputDialog = null,
            )
        }
    }

    public interface Event {
        public fun navigate(structure: ScreenStructure)
        public fun navigateBack()
    }

    private data class ViewModelState(
        val apolloResponseState: ApolloResponseState<ApolloResponse<MoneyUsageScreenQuery.Data>> = ApolloResponseState.loading(),
        val confirmDialog: MoneyUsageScreenUiState.ConfirmDialog? = null,
        val textInputDialog: MoneyUsageScreenUiState.TextInputDialog? = null,
        val calendarDialog: MoneyUsageScreenUiState.CalendarDialog? = null,
        val categorySelectDialog: CategorySelectDialogUiState? = null,
    )
}
