package net.matsudamper.money.frontend.common.viewmodel.moneyusage

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.isFromCache
import com.apollographql.apollo.cache.normalized.watch
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.layout.NumberInputValue
import net.matsudamper.money.frontend.common.ui.screen.moneyusage.MoneyUsageScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.layout.CategorySelectDialogViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EqualsImpl
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.MoneyUsageScreenQuery
import net.matsudamper.money.frontend.graphql.fragment.MoneyUsageScreenMoneyUsage
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState

public class MoneyUsageScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val moneyUsageId: MoneyUsageId,
    private val api: MoneyUsageScreenViewModelApi,
    graphqlClient: GraphqlClient,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val categorySelectDialogViewModel = object {
        private val event: CategorySelectDialogViewModel.Event = object : CategorySelectDialogViewModel.Event {
            override fun selected(result: CategorySelectDialogViewModel.SelectedResult) {
                viewModelScope.launch {
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
            scopedObjectFeature = scopedObjectFeature,
            event = event,
            apolloClient = graphqlClient.apolloClient,
        )
    }.viewModel
    public val uiStateFlow: StateFlow<MoneyUsageScreenUiState> = MutableStateFlow(
        MoneyUsageScreenUiState(
            event = object : MoneyUsageScreenUiState.Event {
                override fun onViewInitialized() {
                    viewModelScope.launch {
                        fetch()
                    }
                }

                override fun onClickRetry() {
                    viewModelScope.launch {
                        fetch()
                    }
                }

                override fun onClickBack() {
                    viewModelScope.launch {
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
            timePickerDialog = null,
            categorySelectDialog = null,
            urlMenuDialog = null,
            numberInputDialog = null,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
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
                                    description = MoneyUsageScreenUiState.Clickable(
                                        text = moneyUsage.description,
                                        event = ClickableEventImpl(moneyUsage.description),
                                    ),
                                    dateTime = Formatter.formatDateTime(moneyUsage.date),
                                    time = Formatter.formatTime(moneyUsage.date.time),
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
                                                viewModelScope.launch {
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
                        timePickerDialog = viewModelState.timePickerDialog,
                        categorySelectDialog = viewModelState.categorySelectDialog,
                        urlMenuDialog = viewModelState.urlMenuDialog,
                        numberInputDialog = viewModelState.numberInputDialog,
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
                                viewModelScope.launch {
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

            override fun onClickCopy() {
                val currentState = viewModelStateFlow.value
                val moneyUsage = when (val state = currentState.apolloResponseState) {
                    is ApolloResponseState.Success -> {
                        state.value.data?.user?.moneyUsage?.moneyUsageScreenMoneyUsage
                    }
                    else -> null
                } ?: return

                viewModelScope.launch {
                    eventSender.send { event ->
                        event.navigate(
                            ScreenStructure.AddMoneyUsage(
                                title = moneyUsage.title,
                                price = moneyUsage.amount.toFloat(),
                                date = moneyUsage.date,
                                description = moneyUsage.description,
                                subCategoryId = moneyUsage.moneyUsageSubCategory?.id?.id?.toString(),
                            ),
                        )
                    }
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
                                viewModelScope.launch {
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
                                viewModelScope.launch {
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

            override fun onClickTimeChange() {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        timePickerDialog = MoneyUsageScreenUiState.TimePickerDialogState(
                            onSelectedTime = { time ->
                                viewModelScope.launch {
                                    val isSuccess = api.updateUsage(
                                        id = moneyUsageId,
                                        date = LocalDateTime(item.date.date, time),
                                    )
                                    if (isSuccess) {
                                        dismissTimePickerDialog()
                                    } else {
                                        // TODO
                                    }
                                }
                            },
                            dismissRequest = { dismissTimePickerDialog() },
                            time = item.date.time,
                        ),
                    )
                }
            }

            override fun onClickAmountChange() {
                viewModelStateFlow.value = viewModelStateFlow.value.copy(
                    numberInputDialog = MoneyUsageScreenUiState.NumberInputDialog(
                        value = NumberInputValue.default(
                            value = item.amount,
                        ),
                        onChangeValue = { value ->
                            viewModelStateFlow.value = viewModelStateFlow.value.copy(
                                numberInputDialog = viewModelStateFlow.value.numberInputDialog?.copy(
                                    value = value,
                                ),
                            )
                        },
                        dismissRequest = {
                            val value = viewModelStateFlow.value.numberInputDialog?.value
                            viewModelStateFlow.value = viewModelStateFlow.value.copy(
                                numberInputDialog = null,
                            )
                            value ?: return@NumberInputDialog
                            viewModelScope.launch {
                                val isSuccess = runCatching {
                                    api.updateUsage(
                                        id = moneyUsageId,
                                        amount = value.value,
                                    )
                                }.fold(
                                    onSuccess = { it },
                                    onFailure = { false },
                                )

                                if (isSuccess) {
                                    viewModelStateFlow.value = viewModelStateFlow.value.copy(
                                        numberInputDialog = null,
                                    )
                                } else {
                                    // TODO
                                }
                            }
                        },
                    ),
                )
            }

            override fun onClickDescription() {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        textInputDialog = MoneyUsageScreenUiState.TextInputDialog(
                            isMultiline = true,
                            title = "説明",
                            onComplete = { text ->
                                viewModelScope.launch {
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

    private val apolloClient = graphqlClient.apolloClient
    private var fetchJob: Job = Job()

    private fun fetch() {
        fetchJob.cancel()
        fetchJob = viewModelScope.launch {
            apolloClient
                .query(MoneyUsageScreenQuery(id = moneyUsageId))
                .fetchPolicy(FetchPolicy.CacheAndNetwork)
                .watch()
                .catch {
                    it.printStackTrace()
                    viewModelStateFlow.update { state ->
                        state.copy(apolloResponseState = ApolloResponseState.failure(it))
                    }
                }
                .collect {
                    if (it.isFromCache && it.data == null) return@collect
                    viewModelStateFlow.update { state ->
                        state.copy(apolloResponseState = ApolloResponseState.success(it))
                    }
                }
        }
    }

    init {
        viewModelScope.launch {
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

    private fun dismissTimePickerDialog() {
        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                timePickerDialog = null,
            )
        }
    }

    private inner class ClickableEventImpl(text: String) : MoneyUsageScreenUiState.ClickableEvent, EqualsImpl(text) {
        override fun onClickUrl(url: String) {
            val dialog = MoneyUsageScreenUiState.UrlMenuDialog(
                url = url,
                event = object : MoneyUsageScreenUiState.UrlMenuDialogEvent {
                    override fun onClickOpen() {
                        viewModelScope.launch {
                            eventSender.send {
                                it.openUrl(url)
                            }
                        }
                        dismiss()
                    }

                    override fun onClickCopy() {
                        viewModelScope.launch {
                            eventSender.send {
                                it.copyUrl(url)
                            }
                        }
                        dismiss()
                    }

                    override fun onDismissRequest() {
                        dismiss()
                    }

                    private fun dismiss() {
                        viewModelStateFlow.update {
                            it.copy(
                                urlMenuDialog = null,
                            )
                        }
                    }
                },
            )
            viewModelScope.launch {
                viewModelStateFlow.update {
                    it.copy(
                        urlMenuDialog = dialog,
                    )
                }
            }
        }

        override fun onLongClickUrl(text: String) {
            viewModelScope.launch {
                eventSender.send {
                    it.copyUrl(text)
                }
            }
        }
    }

    public interface Event {
        public fun navigate(structure: ScreenStructure)

        public fun navigateBack()

        public fun openUrl(text: String)

        public fun copyUrl(text: String)
    }

    private data class ViewModelState(
        val apolloResponseState: ApolloResponseState<ApolloResponse<MoneyUsageScreenQuery.Data>> = ApolloResponseState.loading(),
        val confirmDialog: MoneyUsageScreenUiState.ConfirmDialog? = null,
        val textInputDialog: MoneyUsageScreenUiState.TextInputDialog? = null,
        val calendarDialog: MoneyUsageScreenUiState.CalendarDialog? = null,
        val timePickerDialog: MoneyUsageScreenUiState.TimePickerDialogState? = null,
        val categorySelectDialog: CategorySelectDialogUiState? = null,
        val urlMenuDialog: MoneyUsageScreenUiState.UrlMenuDialog? = null,
        val numberInputDialog: MoneyUsageScreenUiState.NumberInputDialog? = null,
    )
}
