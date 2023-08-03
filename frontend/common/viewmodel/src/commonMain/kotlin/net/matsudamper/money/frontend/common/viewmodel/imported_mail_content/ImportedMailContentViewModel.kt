package net.matsudamper.money.frontend.common.viewmodel.imported_mail_content

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.common.ui.screen.imported_mail_content.ImportedMailContentScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.ImportedMailContentScreenQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState

public class ImportedMailContentViewModel(
    private val id: ImportedMailId,
    private val coroutineScope: CoroutineScope,
    private val api: ImportedMailContentScreenGraphqlApi,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val apolloResponseCollector = api.get(id = id)

    public val uiStateFlow: StateFlow<ImportedMailContentScreenUiState> = MutableStateFlow(
        ImportedMailContentScreenUiState(
            loadingState = ImportedMailContentScreenUiState.LoadingState.Loading,
            event = object : ImportedMailContentScreenUiState.Event {
                override fun onViewInitialized() {
                    fetch()
                }

                override fun onClickClose() {
                    coroutineScope.launch {
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
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val loadingState = when (val resultWrapper = viewModelState.apolloResponseState) {
                    is ApolloResponseState.Failure -> {
                        ImportedMailContentScreenUiState.LoadingState.Error
                    }

                    is ApolloResponseState.Success -> {
                        val html = resultWrapper.value.data?.user?.importedMailAttributes?.mail?.html

                        if (html == null) {
                            ImportedMailContentScreenUiState.LoadingState.Error
                        } else {
                            ImportedMailContentScreenUiState.LoadingState.Loaded(
                                html = html,
                            )
                        }
                    }

                    is ApolloResponseState.Loading -> {
                        ImportedMailContentScreenUiState.LoadingState.Loading
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
        coroutineScope.launch {
            apolloResponseCollector.fetch()
            apolloResponseCollector.flow.collectLatest { apolloResponseState ->
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
        val apolloResponseState: ApolloResponseState<ApolloResponse<ImportedMailContentScreenQuery.Data>> = ApolloResponseState.loading(),
    )
}
