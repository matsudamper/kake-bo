package net.matsudamper.money.frontend.common.viewmodel.importedmail.html

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.ui.screen.importedmail.html.ImportedMailHtmlScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailHtmlScreenQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState

public class ImportedMailHtmlViewModel(
    id: ImportedMailId,
    scopedObjectFeature: ScopedObjectFeature,
    graphqlClient: GraphqlClient,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val apolloResponseCollector = ApolloResponseCollector.create(
        apolloClient = graphqlClient.apolloClient,
        query = ImportedMailHtmlScreenQuery(
            id = id,
        ),
    )

    public val uiStateFlow: StateFlow<ImportedMailHtmlScreenUiState> = MutableStateFlow(
        ImportedMailHtmlScreenUiState(
            loadingState = ImportedMailHtmlScreenUiState.LoadingState.Loading,
            event = object : ImportedMailHtmlScreenUiState.Event {
                override fun onViewInitialized() {
                    fetch()
                }

                override fun onClickClose() {
                    viewModelScope.launch {
                        viewModelEventSender.send {
                            it.backRequest()
                        }
                    }
                }

                override fun onClickRetry() {
                    fetch()
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val loadingState = when (val resultWrapper = viewModelState.apolloResponseState) {
                    is ApolloResponseState.Failure -> {
                        ImportedMailHtmlScreenUiState.LoadingState.Error
                    }

                    is ApolloResponseState.Success -> {
                        val mail = resultWrapper.value.data?.user?.importedMailAttributes?.mail

                        if (mail == null) {
                            ImportedMailHtmlScreenUiState.LoadingState.Error
                        } else {
                            ImportedMailHtmlScreenUiState.LoadingState.Loaded(
                                html = sequence {
                                    yield(mail.html)
                                }.filterNotNull().firstOrNull().orEmpty(),
                            )
                        }
                    }

                    is ApolloResponseState.Loading -> {
                        ImportedMailHtmlScreenUiState.LoadingState.Loading
                    }
                }
                uiStateFlow.update {
                    it.copy(
                        loadingState = loadingState,
                    )
                }
            }
        }
    }.asStateFlow()

    private fun fetch() {
        viewModelScope.launch {
            apolloResponseCollector.fetch()
            apolloResponseCollector.getFlow().collectLatest { apolloResponseState ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        apolloResponseState = apolloResponseState,
                    )
                }
            }
        }
    }

    public interface Event {
        public fun backRequest()
    }

    private data class ViewModelState(
        val apolloResponseState: ApolloResponseState<ApolloResponse<ImportedMailHtmlScreenQuery.Data>> = ApolloResponseState.loading(),
    )
}
