package net.matsudamper.money.frontend.common.viewmodel.importedmail.plain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.common.ui.screen.importedmail.plain.ImportedMailPlainScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.ViewModelFeature
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailPlainScreenQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState

public class ImportedMailPlainViewModel(
    id: ImportedMailId,
    viewModelFeature: ViewModelFeature,
    graphqlClient: GraphqlClient,
) : CommonViewModel(viewModelFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val apolloResponseCollector =
        ApolloResponseCollector.create(
            apolloClient = graphqlClient.apolloClient,
            query =
            ImportedMailPlainScreenQuery(
                id = id,
            ),
        )

    public val uiStateFlow: StateFlow<ImportedMailPlainScreenUiState> =
        MutableStateFlow(
            ImportedMailPlainScreenUiState(
                loadingState = ImportedMailPlainScreenUiState.LoadingState.Loading,
                event =
                object : ImportedMailPlainScreenUiState.Event {
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
                    val loadingState =
                        when (val resultWrapper = viewModelState.apolloResponseState) {
                            is ApolloResponseState.Failure -> {
                                ImportedMailPlainScreenUiState.LoadingState.Error
                            }

                            is ApolloResponseState.Success -> {
                                val mail = resultWrapper.value.data?.user?.importedMailAttributes?.mail

                                if (mail == null) {
                                    ImportedMailPlainScreenUiState.LoadingState.Error
                                } else {
                                    ImportedMailPlainScreenUiState.LoadingState.Loaded(
                                        html =
                                        sequence {
                                            yield(
                                                mail.plain
                                                    ?.replace("\r\n", "<br>")
                                                    ?.replace("\n", "<br>"),
                                            )
                                        }.filterNotNull().firstOrNull().orEmpty(),
                                    )
                                }
                            }

                            is ApolloResponseState.Loading -> {
                                ImportedMailPlainScreenUiState.LoadingState.Loading
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
        val apolloResponseState: ApolloResponseState<ApolloResponse<ImportedMailPlainScreenQuery.Data>> = ApolloResponseState.loading(),
    )
}
