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
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.mail.ImportedMailListScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.mail.ImportedMailListScreenUiState.Filters.LinkStatus.*
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

    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

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
                    if (viewModelStateFlow.value.mailState.mails.isEmpty()) {
                        fetch()
                    }
                }

                override fun moreLoading() {
                    fetch()
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
                            showLastLoading = viewModelState.mailState.finishLoadingToEnd.not(),
                            listItems = viewModelState.mailState.mails.map { mail ->
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
                                status = when (viewModelState.mailState.query.isLinked) {
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
        val newQuery = ViewModelState.Query(
            isLinked = screen.isLinked,
        )
        if (newQuery == viewModelStateFlow.value.mailState.query) {
            return
        }
        viewModelStateFlow.update {
            it.copy(
                mailState = ViewModelState.MailState(
                    query = newQuery,
                ),
            )
        }
        fetch()
    }

    private fun updateLinkStatus(newState: ImportedMailListScreenUiState.Filters.LinkStatus) {
        val isLinked = when (newState) {
            Undefined -> null
            Linked -> true
            NotLinked -> false
        }
        val newQuery = viewModelStateFlow.value.mailState.query.copy(
            isLinked = isLinked,
        )
        coroutineScope.launch {
            viewModelEventSender.send {
                it.changeQuery(isLinked = isLinked)
            }
        }
        if (newQuery == viewModelStateFlow.value.mailState.query) {
            return
        }
        viewModelStateFlow.update {
            it.copy(
                mailState = ViewModelState.MailState(
                    query = newQuery,
                ),
            )
        }
        fetch()
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

            override fun onClick() {
                coroutineScope.launch {
                    viewModelEventSender.send { it.navigateToMailDetail(mail.id) }
                }
            }
        }
    }

    private var fetchJob = Job()
    private fun fetch() {
        val mailState = viewModelStateFlow.value.mailState
        if (mailState.finishLoadingToEnd == true) return
        fetchJob.cancel()
        coroutineScope.launch(
            Job().also { fetchJob = it },
        ) {
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    isLoading = true,
                )
            }

            // TODO Errorhandling
            val response = try {
                runCatching {
                    withContext(ioDispatcher) {
                        graphqlApi.getMail(
                            cursor = mailState.cursor,
                            isLinked = mailState.query.isLinked,
                        )
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
                    mailState = viewModelState.mailState.copy(
                        mails = viewModelState.mailState.mails + mailConnection.nodes,
                        cursor = mailConnection.cursor,
                        finishLoadingToEnd = mailConnection.cursor == null,
                    ),
                    isLoading = false,
                )
            }
        }
    }

    private data class ViewModelState(
        val isLoading: Boolean = true,
        val mailState: MailState = MailState(),
        val fullScreenHtml: String? = null,
    ) {
        data class MailState(
            val cursor: String? = null,
            val finishLoadingToEnd: Boolean = false,
            val mails: List<MailLinkScreenGetMailsQuery.Node> = listOf(),
            val query: Query = Query(),
        )

        data class Query(
            val isLinked: Boolean? = null,
        )
    }
}

// inner classだとis not functionが出るので外に出している
public interface MailLinkViewModelEvent {
    public fun globalToast(message: String)
    public fun changeQuery(isLinked: Boolean?)
    public fun navigateToMailDetail(id: ImportedMailId)
}
