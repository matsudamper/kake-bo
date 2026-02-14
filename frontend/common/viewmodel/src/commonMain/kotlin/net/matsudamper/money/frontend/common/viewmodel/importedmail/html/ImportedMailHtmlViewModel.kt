package net.matsudamper.money.frontend.common.viewmodel.importedmail.html

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.ui.screen.importedmail.html.ImportedMailHtmlScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailHtmlScreenQuery

public class ImportedMailHtmlViewModel(
    private val id: ImportedMailId,
    scopedObjectFeature: ScopedObjectFeature,
    private val graphqlClient: GraphqlClient,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

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
                val apolloResult = viewModelState.apolloResponseState

                if (apolloResult == null) {
                    uiStateFlow.update {
                        it.copy(
                            loadingState = ImportedMailHtmlScreenUiState.LoadingState.Loading,
                        )
                    }
                    return@collectLatest
                }

                if (apolloResult.isFailure) {
                    uiStateFlow.update {
                        it.copy(
                            loadingState = ImportedMailHtmlScreenUiState.LoadingState.Error,
                        )
                    }
                    return@collectLatest
                }

                val response = apolloResult.getOrThrow()
                if (response.hasErrors()) {
                    uiStateFlow.update {
                        it.copy(
                            loadingState = ImportedMailHtmlScreenUiState.LoadingState.Error,
                        )
                    }
                    return@collectLatest
                }

                val mailData = response.data?.user?.importedMailAttributes?.mail
                if (mailData == null) {
                    uiStateFlow.update {
                        it.copy(
                            loadingState = ImportedMailHtmlScreenUiState.LoadingState.Error,
                        )
                    }
                    return@collectLatest
                }

                val html = mailData.html ?: ""
                uiStateFlow.update {
                    it.copy(
                        loadingState = ImportedMailHtmlScreenUiState.LoadingState.Loaded(
                            html = html,
                        ),
                    )
                }
            }
        }
    }.asStateFlow()

    private fun fetch() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                graphqlClient.apolloClient.query(
                    ImportedMailHtmlScreenQuery(
                        id = id,
                    ),
                )
                    .fetchPolicy(FetchPolicy.NetworkOnly)
                    .execute()
            }

            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    apolloResponseState = result,
                )
            }
        }
    }

    public interface Event {
        public fun backRequest()
    }

    private data class ViewModelState(
        val apolloResponseState: Result<ApolloResponse<ImportedMailHtmlScreenQuery.Data>>? = null,
    )
}
