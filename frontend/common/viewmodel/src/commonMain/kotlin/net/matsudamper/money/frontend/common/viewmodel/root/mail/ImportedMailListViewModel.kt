package net.matsudamper.money.frontend.common.viewmodel.root.mail

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.mail.ImportedMailListScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.mail.ImportedMailListScreenUiState.Filters.LinkStatus
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.PlatformType
import net.matsudamper.money.frontend.common.viewmodel.PlatformTypeProvider
import net.matsudamper.money.frontend.common.viewmodel.RootScreenScaffoldListenerDefaultImpl
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.ImportedMailListScreenMailPagingQuery
import net.matsudamper.money.frontend.graphql.MailLinkScreenGraphqlApi

public class ImportedMailListViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val ioDispatcher: CoroutineDispatcher,
    private val graphqlApi: MailLinkScreenGraphqlApi,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelEventSender = EventSender<MailLinkViewModelEvent>()
    public val eventHandler: EventHandler<MailLinkViewModelEvent> = viewModelEventSender.asHandler()

    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    private val composeOperation = Channel<(ImportedMailListScreenUiState.Operation) -> Unit>(Channel.BUFFERED)
    public val rootUiStateFlow: StateFlow<ImportedMailListScreenUiState> = MutableStateFlow(
        ImportedMailListScreenUiState(
            operation = composeOperation.receiveAsFlow(),
            filters = ImportedMailListScreenUiState.Filters(
                link = ImportedMailListScreenUiState.Filters.Link(
                    status = LinkStatus.Undefined,
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

                override fun refresh() {
                    viewModelStateFlow.update {
                        it.copy(
                            mailState = it.mailState.copy(
                                cursor = null,
                                finishLoadingToEnd = false,
                                mails = listOf(),
                            ),
                        )
                    }
                    fetch()
                }
            },
            loadingState = ImportedMailListScreenUiState.LoadingState.Loading,
            rootScreenScaffoldListener = object : RootScreenScaffoldListenerDefaultImpl(navController) {
                override fun onClickAdd() {
                    if (PlatformTypeProvider.type == PlatformType.JS) {
                        super.onClickAdd()
                    } else {
                        composeOperation.trySend {
                            it.scrollToTop()
                        }
                    }
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
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
                                            service = usage.serviceName.orEmpty(),
                                            amount = run price@{
                                                val amount = usage.amount ?: return@price ""
                                                "${Formatter.formatMoney(amount)}円"
                                            },
                                            dateTime = run date@{
                                                val dateTime = usage.dateTime ?: return@date ""
                                                Formatter.formatDateTime(dateTime)
                                            },
                                            category = run category@{
                                                val subCategory = usage.subCategory ?: return@category ""
                                                val category = subCategory.category

                                                "${category.name} / ${subCategory.name}"
                                            },
                                        )
                                    }.toImmutableList(),
                                    event = createMailEvent(mail = mail),
                                )
                            }.toImmutableList(),
                        ),
                        filters = uiState.filters.copy(
                            link = uiState.filters.link.copy(
                                status = when (viewModelState.mailState.query.isLinked) {
                                    null -> LinkStatus.Undefined
                                    true -> LinkStatus.Linked
                                    false -> LinkStatus.NotLinked
                                },
                            ),
                        ),
                    )
                }
            }
        }
    }.asStateFlow()

    public fun updateQuery(screen: ScreenStructure.Root.Add.Imported) {
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

    private fun updateLinkStatus(newState: LinkStatus) {
        val isLinked = when (newState) {
            LinkStatus.Undefined -> null
            LinkStatus.Linked -> true
            LinkStatus.NotLinked -> false
        }
        val newQuery = viewModelStateFlow.value.mailState.query.copy(
            isLinked = isLinked,
        )
        viewModelScope.launch {
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

    private fun createMailEvent(mail: ImportedMailListScreenMailPagingQuery.Node): ImportedMailListScreenUiState.ListItemEvent {
        return object : ImportedMailListScreenUiState.ListItemEvent {
            override fun onClickMailDetail() {
                viewModelScope.launch {
                    viewModelEventSender.send {
                        it.navigateToMailContent(mail.id)
                    }
                }
            }

            override fun onClick() {
                viewModelScope.launch {
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
        viewModelScope.launch(
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
    ) {
        data class MailState(
            val cursor: String? = null,
            val finishLoadingToEnd: Boolean = false,
            val mails: List<ImportedMailListScreenMailPagingQuery.Node> = listOf(),
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

    public fun navigateToMailContent(id: ImportedMailId)
}
