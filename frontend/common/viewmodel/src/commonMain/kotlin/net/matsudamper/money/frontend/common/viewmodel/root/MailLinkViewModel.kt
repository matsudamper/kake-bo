package net.matsudamper.money.frontend.common.viewmodel.root

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.screen.tmp_mail.MailLinkScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.MailLinkScreenGetMailsQuery
import net.matsudamper.money.frontend.graphql.MailLinkScreenGraphqlApi

public class MailLinkViewModel(
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val graphqlApi: MailLinkScreenGraphqlApi,
) {
    private val viewModelEventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val rootUiStateFlow: StateFlow<MailLinkScreenUiState> = MutableStateFlow(
        MailLinkScreenUiState(
            event = object : MailLinkScreenUiState.Event {
                override fun onClickBackButton() {
                    coroutineScope.launch {
                        viewModelEventSender.send { it.backRequest() }
                    }
                }

                override fun onViewInitialized() {
                    if (viewModelStateFlow.value.mails.isEmpty()) {
                        fetch()
                    }
                }

                override fun dismissFullScreenHtml() {
                    coroutineScope.launch {
                        viewModelStateFlow.update {
                            it.copy(
                                fullScreenHtml = null,
                            )
                        }
                    }
                }
            },
            fullScreenHtml = null,
            loadingState = MailLinkScreenUiState.LoadingState.Loading,
        ),
    ).also {
        coroutineScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                it.update {
                    it.copy(
                        fullScreenHtml = viewModelState.fullScreenHtml,
                        loadingState = MailLinkScreenUiState.LoadingState.Loaded(
                            mails = viewModelState.mails.map { mail ->
                                MailLinkScreenUiState.Mail(
                                    mailFrom = mail.from,
                                    mailSubject = mail.subject,
                                    title = mail.suggestUsage?.title.orEmpty(),
                                    description = mail.suggestUsage?.description.orEmpty(),
                                    service = mail.suggestUsage?.service?.name.orEmpty(),
                                    price = run price@{
                                        val price = mail.suggestUsage?.price ?: return@price ""
                                        "${price}円"
                                    },
                                    date = mail.suggestUsage?.date?.toString().orEmpty(),
                                    event = createMailEvent(mail = mail),
                                )
                            }.toImmutableList(),
                        ),
                    )
                }
            }
        }
    }.asStateFlow()

    private fun createMailEvent(mail: MailLinkScreenGetMailsQuery.Node): MailLinkScreenUiState.MailEvent {
        return object : MailLinkScreenUiState.MailEvent {
            override fun onClickMailDetail() {
                coroutineScope.launch {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            fullScreenHtml = mail.html ?: mail.plain
                                ?.replace("\r\n", "<br>")
                                ?.replace("\n", "<br>"),
                        )
                    }
                }
            }

            override fun onClickImportSuggestDetailButton() {
                coroutineScope.launch {
                    viewModelEventSender.send { it.globalToast("未実装") }
                }
            }
        }
    }

    private var fetchJob = Job()
    private fun fetch() {
        if (viewModelStateFlow.value.finishLoadingToEnd == true) return
        fetchJob.cancel()
        coroutineScope.launch(
            Job().also { fetchJob = it },
        ) {
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    isLoading = true,
                )
            }

            val response = try {
                runCatching {
                    withContext(ioDispatcher) {
                        graphqlApi.getMail(viewModelStateFlow.value.cursor)
                    }
                }.getOrNull() ?: return@launch
            } finally {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        isLoading = false,
                    )
                }
            }

            val mailConnection = response.data?.user?.importedMailAttributes?.mails
                ?: return@launch

            if (isActive.not()) return@launch
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    mails = viewModelState.mails + mailConnection.nodes,
                    cursor = mailConnection.cursor,
                    finishLoadingToEnd = mailConnection.cursor == null,
                    isLoading = false,
                )
            }
        }
    }

    public interface Event {
        public fun backRequest()
        public fun globalToast(message: String)
    }

    private data class ViewModelState(
        val isLoading: Boolean = true,
        val cursor: String? = null,
        val finishLoadingToEnd: Boolean? = null,
        val mails: List<MailLinkScreenGetMailsQuery.Node> = listOf(),
        val fullScreenHtml: String? = null,
    )
}
