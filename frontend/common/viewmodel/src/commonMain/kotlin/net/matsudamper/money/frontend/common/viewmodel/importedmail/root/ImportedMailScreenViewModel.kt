package net.matsudamper.money.frontend.common.viewmodel.importedmail.root

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.importedmail.root.MailScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.ImportedMailScreenQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState

public class ImportedMailScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val api: ImportedMailScreenGraphqlApi,
    private val importedMailId: ImportedMailId,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val apolloResponseCollector = api.get(id = importedMailId, "1")
    private val event = object : MailScreenUiState.Event {
        override fun onClickRetry() {
            fetch()
        }

        override fun onClickArrowBackButton() {
            coroutineScope.launch {
                viewModelEventSender.send {
                    it.navigateToBack()
                }
            }
        }

        override fun onClickTitle() {
            coroutineScope.launch {
                viewModelEventSender.send {
                    it.navigateToHome()
                }
            }
        }

        override fun onClickDelete() {
            coroutineScope.launch {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        confirmDialog = MailScreenUiState.AlertDialog(
                            onDismissRequest = { dismissConfirmDialog() },
                            onClickNegative = { dismissConfirmDialog() },
                            onClickPositive = {
                                coroutineScope.launch {
                                    val isSuccess = api.delete(id = importedMailId)
                                    if (isSuccess) {
                                        dismissConfirmDialog()
                                        viewModelEventSender.send { it.navigateToBack() }
                                    }
                                }
                            },
                            title = "削除しますか？",
                        ),
                    )
                }
            }
        }
    }

    public val uiStateFlow: StateFlow<MailScreenUiState> = MutableStateFlow(
        MailScreenUiState(
            loadingState = MailScreenUiState.LoadingState.Loading,
            event = event,
            confirmDialog = null,
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        confirmDialog = viewModelState.confirmDialog,
                        loadingState = when (val apolloResponse = viewModelState.apolloResponse) {
                            is ApolloResponseState.Failure -> {
                                MailScreenUiState.LoadingState.Error
                            }

                            is ApolloResponseState.Loading -> {
                                MailScreenUiState.LoadingState.Loading
                            }

                            is ApolloResponseState.Success -> {
                                val mail = apolloResponse.value.data?.user?.importedMailAttributes?.mail
                                if (mail == null) {
                                    MailScreenUiState.LoadingState.Loading
                                } else {
                                    createLoadedUiState(mail = mail)
                                }
                            }
                        },
                    )
                }
            }
        }
    }.asStateFlow()

    private fun createLoadedUiState(mail: ImportedMailScreenQuery.Mail): MailScreenUiState.LoadingState.Loaded {
        return MailScreenUiState.LoadingState.Loaded(
            mail = MailScreenUiState.Mail(
                title = mail.subject,
                date = Formatter.formatDateTime(mail.dateTime),
                from = mail.from,
            ),
            usage = mail.usages.map {
                MailScreenUiState.LinkedUsage(
                    title = it.title,
                    amount = run amount@{
                        val splitAmount = Formatter.formatMoney(it.amount)
                        "${splitAmount}円"
                    },
                    category = run category@{
                        val subCategory = it.moneyUsageSubCategory ?: return@category null
                        val category = subCategory.category

                        "${category.name} / ${subCategory.name}"
                    },
                    date = Formatter.formatDateTime(it.date),
                )
            }.toImmutableList(),
            usageSuggest = mail.suggestUsages.mapIndexed { index, suggestUsage ->
                MailScreenUiState.UsageSuggest(
                    title = suggestUsage.title,
                    serviceName = suggestUsage.serviceName.orEmpty(),
                    amount = run amount@{
                        val amount = suggestUsage.amount ?: return@amount null

                        val splitAmount = Formatter.formatMoney(amount)
                        "${splitAmount}円"
                    },
                    category = run category@{
                        val subCategory =
                            suggestUsage.subCategory ?: return@category null
                        val category = subCategory.category

                        "${category.name} / ${subCategory.name}"
                    },
                    description = run {
                        MailScreenUiState.Clickable(
                            text = suggestUsage.description,
                            onClickUrl = { url ->
                                coroutineScope.launch {
                                    viewModelEventSender.send {
                                        it.openWeb(url)
                                    }
                                }
                            },
                        )
                    },
                    dateTime = run dateTime@{
                        val dateTIme = suggestUsage.dateTime ?: return@dateTime ""
                        Formatter.formatDateTime(dateTIme)
                    },
                    event = object : MailScreenUiState.UsageSuggest.Event {
                        override fun onClickRegister() {
                            coroutineScope.launch {
                                viewModelEventSender.send {
                                    it.navigate(
                                        ScreenStructure.AddMoneyUsage(
                                            importedMailId = importedMailId,
                                            importedMailIndex = index,
                                        ),
                                    )
                                }
                            }
                        }
                    },
                )
            }.toImmutableList(),
            hasHtml = mail.hasHtml,
            hasPlain = mail.hasPlain,
            event = object : MailScreenUiState.LoadedEvent {
                override fun onClickMailHtml() {
                    coroutineScope.launch {
                        viewModelEventSender.send {
                            it.navigate(
                                ScreenStructure.ImportedMailHTML(
                                    id = importedMailId,
                                ),
                            )
                        }
                    }
                }

                override fun onClickMailPlain() {
                    coroutineScope.launch {
                        viewModelEventSender.send {
                            it.navigate(
                                ScreenStructure.ImportedMailPlain(
                                    id = importedMailId,
                                ),
                            )
                        }
                    }
                }

                override fun onClickRegister() {
                    coroutineScope.launch {
                        viewModelEventSender.send {
                            it.navigate(
                                ScreenStructure.AddMoneyUsage(
                                    importedMailId = importedMailId,
                                ),
                            )
                        }
                    }
                }
            },
        )
    }

    init {
        fetch()
        coroutineScope.launch {
            apolloResponseCollector.flow.collectLatest { response ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        apolloResponse = response,
                    )
                }
            }
        }
    }

    private fun fetch() {
        coroutineScope.launch {
            apolloResponseCollector.fetch(this)
        }
    }

    private fun dismissConfirmDialog() {
        coroutineScope.launch {
            viewModelStateFlow.update {
                it.copy(
                    confirmDialog = null,
                )
            }
        }
    }

    public interface Event {
        public fun navigateToBack()
        public fun navigateToHome()
        public fun navigate(screenStructure: ScreenStructure)
        public fun openWeb(url: String)
    }

    private data class ViewModelState(
        val isLoading: Boolean = true,
        val apolloResponse: ApolloResponseState<ApolloResponse<ImportedMailScreenQuery.Data>> = ApolloResponseState.loading(),
        val confirmDialog: MailScreenUiState.AlertDialog? = null,
    )
}
