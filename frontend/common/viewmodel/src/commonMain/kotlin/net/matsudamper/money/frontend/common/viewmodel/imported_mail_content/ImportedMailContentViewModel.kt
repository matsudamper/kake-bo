package net.matsudamper.money.frontend.common.viewmodel.imported_mail_content

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.common.ui.screen.imported_mail_content.ImportedMailContentScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.ImportedMailContentScreenQuery
import net.matsudamper.money.lib.ResultWrapper

public class ImportedMailContentViewModel(
    private val id: ImportedMailId,
    private val coroutineScope: CoroutineScope,
    private val api: ImportedMailContentScreenGraphqlApi,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    public val uiStateFlow: StateFlow<ImportedMailContentScreenUiState> = MutableStateFlow(
        ImportedMailContentScreenUiState(
            loadingState = ImportedMailContentScreenUiState.LoadingState.Loading,
            event = object : ImportedMailContentScreenUiState.Event {
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
                println("viewModelStateFlow.collectLatest")
                viewModelState.resultFlow
//                    .stateIn(coroutineScope, SharingStarted.Lazily, null)
                    .collectLatest { resultWrapper ->
                        println("resultFlow.collectLatest")
                        val loadingState = when (resultWrapper) {
                            is ResultWrapper.Failure -> {
                                ImportedMailContentScreenUiState.LoadingState.Error
                            }

                            is ResultWrapper.Success -> {
                                val html = resultWrapper.value.data?.user?.importedMailAttributes?.mail?.html

                                if (html == null) {
                                    ImportedMailContentScreenUiState.LoadingState.Error
                                } else {
                                    ImportedMailContentScreenUiState.LoadingState.Loaded(
                                        html = html,
                                    )
                                }
                            }

                            null -> {
                                ImportedMailContentScreenUiState.LoadingState.Loading
                            }
                        }
                        println(loadingState.toString())
                        uiStateFlow.update {
                            it.copy(
                                loadingState = loadingState,
                            )
                        }
                    }
            }
        }
    }.asStateFlow()

    init {
        fetch()
    }

    private fun fetch() {
        println("fetch0")
        viewModelStateFlow.update {
            it.copy(
                resultFlow = api.get(id = id),
            )
        }
        println("fetch1")
    }

    public interface Event {
        public fun backRequest()
    }

    private data class ViewModelState(
        val resultFlow: Flow<ResultWrapper<ApolloResponse<ImportedMailContentScreenQuery.Data>>> = flowOf(),
    )
}
