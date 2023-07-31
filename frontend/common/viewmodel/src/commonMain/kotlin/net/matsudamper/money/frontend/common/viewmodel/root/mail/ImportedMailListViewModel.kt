package net.matsudamper.money.frontend.common.viewmodel.root.mail

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
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.mail.ImportedMailListScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.mail.ImportedMailListScreenUiState.Filters.LinkStatus.*
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.MailLinkScreenGetMailsQuery
import net.matsudamper.money.frontend.graphql.MailLinkScreenGraphqlApi

public class ImportedMailListViewModel(
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val graphqlApi: MailLinkScreenGraphqlApi,
) {
    private val viewModelEventSender = EventSender<MailLinkViewModelEvent>()
    public val eventHandler: EventHandler<MailLinkViewModelEvent> = viewModelEventSender.asHandler()

    private val viewModelStateFlow = MutableStateFlow(
        ViewModelState(
            query = ViewModelState.Query(
                isLinked = null,
            ),
        ),
    )

    public val rootUiStateFlow: StateFlow<ImportedMailListScreenUiState> = MutableStateFlow(
        ImportedMailListScreenUiState(
            filters = ImportedMailListScreenUiState.Filters(
                link = ImportedMailListScreenUiState.Filters.Link(
                    status = Undefined,
                    updateState = { updateLinkStatus(it) },
                ),
            ),
            event = object : ImportedMailListScreenUiState.Event {
                override fun onViewInitialized() {
                    if (viewModelStateFlow.value.mails.isEmpty()) {
                        fetch()
                    }
                }

                override fun dismissFullScreenHtml() {
                    viewModelStateFlow.update {
                        it.copy(
                            fullScreenHtml = null,
                        )
                    }
                }
            },
            fullScreenHtml = null,
            loadingState = ImportedMailListScreenUiState.LoadingState.Loading,
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        fullScreenHtml = viewModelState.fullScreenHtml,
                        loadingState = ImportedMailListScreenUiState.LoadingState.Loaded(
                            listItems = viewModelState.mails.map { mail ->
                                ImportedMailListScreenUiState.ListItem(
                                    mail = ImportedMailListScreenUiState.ImportedMail(
                                        mailFrom = mail.from,
                                        mailSubject = mail.subject,
                                    ),
                                    usages = mail.suggestUsages.map { usage ->
                                        ImportedMailListScreenUiState.UsageItem(
                                            title = usage.title,
                                            description = usage.description,
                                            service = usage.service?.name.orEmpty(),
                                            price = run price@{
                                                val price = usage.price ?: return@price ""
                                                "${price}円"
                                            },
                                            date = usage.date?.toString().orEmpty(),
                                        )
                                    }.toImmutableList(),
                                    event = createMailEvent(mail = mail),
                                )
                            }.toImmutableList(),
                        ),
                        filters = uiState.filters.copy(
                            link = uiState.filters.link.copy(
                                status = when (viewModelState.query.isLinked) {
                                    null -> Undefined
                                    true -> Linked
                                    false -> NotLinked
                                },
                            ),
                        ),
                    )
                }
            }
        }
    }.asStateFlow()

    public fun updateQuery(
        screen: ScreenStructure.Root.Mail.Imported,
    ) {
        viewModelStateFlow.update {
            it.copy(
                query = it.query.copy(
                    isLinked = screen.isLinked,
                ),
            )
        }
    }

    private fun updateLinkStatus(newState: ImportedMailListScreenUiState.Filters.LinkStatus) {
        val isLinked = when (newState) {
            Undefined -> null
            Linked -> true
            NotLinked -> false
        }
        coroutineScope.launch {
            viewModelEventSender.send {
                it.changeQuery(isLinked = isLinked)
            }
        }
        viewModelStateFlow.update {
            it.copy(
                query = it.query.copy(
                    isLinked = isLinked,
                ),
            )
        }
    }

    private fun createMailEvent(mail: MailLinkScreenGetMailsQuery.Node): ImportedMailListScreenUiState.ListItemEvent {
        return object : ImportedMailListScreenUiState.ListItemEvent {
            override fun onClickMailDetail() {
                coroutineScope.launch {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            // TODO 表示する時にデータを取得するようにする
                            fullScreenHtml = mail.html ?: mail.plain
                                ?.replace("\r\n", "<br>")
                                ?.replace("\n", "<br>"),
                        )
                    }
                }
            }

            override fun onClickRegisterButton() {
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

    private data class ViewModelState(
        val isLoading: Boolean = true,
        val cursor: String? = null,
        val finishLoadingToEnd: Boolean? = null,
        val mails: List<MailLinkScreenGetMailsQuery.Node> = listOf(),
        val fullScreenHtml: String? = null,
        val query: Query,
    ) {
        public data class Query(
            val isLinked: Boolean?,
        )
    }
}

// inner classだとis not functionが出るので外に出している
public interface MailLinkViewModelEvent {
    public fun globalToast(message: String)
    public fun changeQuery(isLinked: Boolean?)
}
