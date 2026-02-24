package net.matsudamper.money.frontend.common.viewmodel.importedmail.root

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo.api.ApolloResponse
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.importedmail.root.MailScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EqualsImpl
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.ImportedMailScreenQuery

public class ImportedMailScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val api: ImportedMailScreenGraphqlApi,
    private val importedMailId: ImportedMailId,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()
    private val event = object : MailScreenUiState.Event {
        override fun onClickRetry() {
            fetch()
        }

        override fun onClickArrowBackButton() {
            viewModelScope.launch {
                viewModelEventSender.send {
                    it.navigateToBack()
                }
            }
        }

        override fun onClickTitle() {
            viewModelScope.launch {
                viewModelEventSender.send {
                    it.navigateToHome()
                }
            }
        }

        override fun onClickDelete() {
            viewModelScope.launch {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        confirmDialog = MailScreenUiState.AlertDialog(
                            onDismissRequest = { dismissConfirmDialog() },
                            onClickNegative = { dismissConfirmDialog() },
                            onClickPositive = {
                                viewModelScope.launch {
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

        override fun onResume() {
            fetch()
        }
    }

    public val uiStateFlow: StateFlow<MailScreenUiState> = MutableStateFlow(
        MailScreenUiState(
            loadingState = MailScreenUiState.LoadingState.Loading,
            event = event,
            confirmDialog = null,
            urlMenuDialog = null,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        confirmDialog = viewModelState.confirmDialog,
                        urlMenuDialog = viewModelState.urlMenuDialog,
                        loadingState = run {
                            val apolloResult = viewModelState.apolloResponse
                            if (apolloResult == null) {
                                return@run MailScreenUiState.LoadingState.Loading
                            }

                            if (apolloResult.isFailure) {
                                return@run MailScreenUiState.LoadingState.Error
                            }

                            val response = apolloResult.getOrThrow()
                            val mail = response.data?.user?.importedMailAttributes?.mail

                            if (response.hasErrors() || mail == null) {
                                return@run MailScreenUiState.LoadingState.Error
                            }

                            createLoadedUiState(mail = mail)
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
                    id = it.id.toString(),
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
                    event = object : MailScreenUiState.LinkedUsageEvent {
                        override fun onClick() {
                            viewModelScope.launch {
                                viewModelEventSender.send { event ->
                                    event.navigate(
                                        ScreenStructure.MoneyUsage(
                                            id = it.id,
                                        ),
                                    )
                                }
                            }
                        }
                    },
                )
            }.toImmutableList(),
            usageSuggest = mail.suggestUsages.mapIndexed { index, suggestUsage ->
                MailScreenUiState.UsageSuggest(
                    id = index.toString(),
                    title = suggestUsage.title,
                    serviceName = suggestUsage.serviceName.orEmpty(),
                    amount = run amount@{
                        val amount = suggestUsage.amount ?: return@amount null

                        val splitAmount = Formatter.formatMoney(amount)
                        "${splitAmount}円"
                    },
                    category = run category@{
                        val subCategory = suggestUsage.subCategory ?: return@category null
                        val category = subCategory.category

                        "${category.name} / ${subCategory.name}"
                    },
                    description = run {
                        MailScreenUiState.Clickable(
                            text = suggestUsage.description,
                            event = ClickableEventImpl(suggestUsage.description),
                        )
                    },
                    dateTime = run dateTime@{
                        val dateTIme = suggestUsage.dateTime ?: return@dateTime ""
                        Formatter.formatDateTime(dateTIme)
                    },
                    event = object : MailScreenUiState.UsageSuggest.Event {
                        override fun onClickRegister() {
                            viewModelScope.launch {
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
                    viewModelScope.launch {
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
                    viewModelScope.launch {
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
                    viewModelScope.launch {
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
    }

    private fun fetch() {
        viewModelScope.launch {
            val result = api.get(id = importedMailId)

            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    apolloResponse = result,
                )
            }
        }
    }

    private fun dismissConfirmDialog() {
        viewModelScope.launch {
            viewModelStateFlow.update {
                it.copy(
                    confirmDialog = null,
                )
            }
        }
    }

    private inner class ClickableEventImpl(
        private val text: String,
    ) : MailScreenUiState.ClickableEvent, EqualsImpl(text) {
        override fun onClickUrl(url: String) {
            val dialog = MailScreenUiState.UrlMenuDialog(
                url = url,
                event = object : MailScreenUiState.UrlMenuDialogEvent {
                    override fun onClickOpen() {
                        viewModelScope.launch {
                            viewModelEventSender.send {
                                it.openWeb(text)
                            }
                        }
                        dismiss()
                    }

                    override fun onClickCopy() {
                        viewModelScope.launch {
                            viewModelEventSender.send {
                                it.copyToClipboard(text)
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
            viewModelStateFlow.update {
                it.copy(
                    urlMenuDialog = dialog,
                )
            }
        }

        // スマホだと長押しが効かない: 1.5.10-rc01
        override fun onLongClickUrl(text: String) {
            viewModelScope.launch {
                viewModelEventSender.send {
                    it.copyToClipboard(text)
                }
            }
        }
    }

    public interface Event {
        public fun navigateToBack()

        public fun navigateToHome()

        public fun navigate(screenStructure: ScreenStructure)

        public fun openWeb(url: String)

        public fun copyToClipboard(text: String)
    }

    private data class ViewModelState(
        val isLoading: Boolean = true,
        val apolloResponse: Result<ApolloResponse<ImportedMailScreenQuery.Data>>? = null,
        val confirmDialog: MailScreenUiState.AlertDialog? = null,
        val urlMenuDialog: MailScreenUiState.UrlMenuDialog? = null,
    )
}
