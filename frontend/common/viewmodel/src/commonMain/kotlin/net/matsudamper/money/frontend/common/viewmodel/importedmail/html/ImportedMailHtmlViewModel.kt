package net.matsudamper.money.frontend.common.viewmodel.importedmail.html

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
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
                val response = viewModelState.apolloResponseState
                val mailData = response?.data?.user?.importedMailAttributes?.mail
                val loadingState = if (viewModelState.apolloResponseState == null) {
                    ImportedMailHtmlScreenUiState.LoadingState.Loading
                } else if (
                    response.hasErrors() ||
                    mailData == null
                ) {
                    ImportedMailHtmlScreenUiState.LoadingState.Error
                } else {
                    ImportedMailHtmlScreenUiState.LoadingState.Loaded(
                        html = sequence {
                            yield(mailData.html)
                        }.filterNotNull().firstOrNull().orEmpty(),
                    )
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
        viewModelScope.launch(Dispatchers.IO) {
            val response = graphqlClient.apolloClient.query(
                ImportedMailHtmlScreenQuery(
                    id = id,
                ),
            )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    apolloResponseState = response,
                )
            }
        }
    }

    public interface Event {
        public fun backRequest()
    }

    private data class ViewModelState(
        val apolloResponseState: ApolloResponse<ImportedMailHtmlScreenQuery.Data>? = null,
    )
}
