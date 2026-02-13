package net.matsudamper.money.frontend.common.viewmodel.importedmail.plain

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
import net.matsudamper.money.frontend.common.ui.screen.importedmail.plain.ImportedMailPlainScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailPlainScreenQuery

public class ImportedMailPlainViewModel(
    private val id: ImportedMailId,
    scopedObjectFeature: ScopedObjectFeature,
    private val graphqlClient: GraphqlClient,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    public val uiStateFlow: StateFlow<ImportedMailPlainScreenUiState> = MutableStateFlow(
        ImportedMailPlainScreenUiState(
            loadingState = ImportedMailPlainScreenUiState.LoadingState.Loading,
            event = object : ImportedMailPlainScreenUiState.Event {
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
                val loadingState: ImportedMailPlainScreenUiState.LoadingState

                val apolloResult = viewModelState.apolloResponseState
                if (apolloResult == null) {
                    loadingState = ImportedMailPlainScreenUiState.LoadingState.Loading
                    uiStateFlow.update { it.copy(loadingState = loadingState) }
                    return@collectLatest
                }

                if (apolloResult.isFailure) {
                    loadingState = ImportedMailPlainScreenUiState.LoadingState.Error
                    uiStateFlow.update { it.copy(loadingState = loadingState) }
                    return@collectLatest
                }

                val response = apolloResult.getOrThrow()
                val mailData = response.data?.user?.importedMailAttributes?.mail

                if (response.hasErrors() || mailData == null) {
                    loadingState = ImportedMailPlainScreenUiState.LoadingState.Error
                    uiStateFlow.update { it.copy(loadingState = loadingState) }
                    return@collectLatest
                }

                loadingState = ImportedMailPlainScreenUiState.LoadingState.Loaded(
                    html = sequence {
                        yield(
                            mailData.plain
                                ?.replace("\r\n", "<br>")
                                ?.replace("\n", "<br>"),
                        )
                    }.filterNotNull().firstOrNull().orEmpty(),
                )

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
            val result = runCatching {
                graphqlClient.apolloClient.query(
                    ImportedMailPlainScreenQuery(
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
        val apolloResponseState: Result<ApolloResponse<ImportedMailPlainScreenQuery.Data>>? = null,
    )
}
