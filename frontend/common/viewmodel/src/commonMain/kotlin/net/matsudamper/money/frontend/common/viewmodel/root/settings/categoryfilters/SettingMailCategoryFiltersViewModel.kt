package net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilters

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.settings.SettingMailCategoryFilterScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFiltersScreenPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState

public class SettingMailCategoryFiltersViewModel(
    private val pagingModel: ImportedMailCategoryFilterScreenPagingModel,
    private val api: SettingImportedMailCategoryFilterApi,
    scopedObjectFeature: ScopedObjectFeature,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(ViewModelState())

    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val loadedEvent =
        object : SettingMailCategoryFilterScreenUiState.LoadedEvent {
            override fun onClickAdd() {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        textInputDialog = createFilterNameDialogUiState(),
                    )
                }
            }

            private fun createFilterNameDialogUiState(): SettingMailCategoryFilterScreenUiState.TextInput {
                return SettingMailCategoryFilterScreenUiState.TextInput(
                    title = "メールカテゴリフィルタの追加",
                    onCompleted = { text ->
                        viewModelScope.launch {
                            runCatching {
                                api.addFilter(text)
                            }.onSuccess {
                                pagingModel.clear()
                                pagingModel.fetch()
                            }.onFailure {
                                it.printStackTrace()
                            }
                        }
                        dismissTextInputDialog()
                    },
                    dismiss = {
                        dismissTextInputDialog()
                    },
                )
            }

            private fun dismissTextInputDialog() {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        textInputDialog = null,
                    )
                }
            }
        }

    public val uiStateFlow: StateFlow<SettingMailCategoryFilterScreenUiState> =
        MutableStateFlow(
            SettingMailCategoryFilterScreenUiState(
                loadingState = SettingMailCategoryFilterScreenUiState.LoadingState.Loading,
                textInput = null,
                event =
                object : SettingMailCategoryFilterScreenUiState.Event {
                    override fun onClickRetry() {
                        viewModelScope.launch {
                            pagingModel.fetch()
                        }
                    }

                    override fun onViewInitialized() {
                        viewModelScope.launch {
                            pagingModel.fetch()
                        }
                    }

                    override fun onClickBack() {
                        viewModelScope.launch {
                            eventSender.send {
                                it.navigate(ScreenStructure.Root.Settings.Root)
                            }
                        }
                    }
                },
            ),
        ).also { uiStateFlow ->
            viewModelScope.launch {
                viewModelStateFlow.collectLatest { viewModelState ->
                    uiStateFlow.update { uiState ->
                        val loadingState =
                            when (val last = viewModelState.apolloResponseStates.lastOrNull()) {
                                null,
                                is ApolloResponseState.Loading,
                                -> {
                                    SettingMailCategoryFilterScreenUiState.LoadingState.Loading
                                }

                                is ApolloResponseState.Failure -> {
                                    SettingMailCategoryFilterScreenUiState.LoadingState.Error
                                }

                                is ApolloResponseState.Success -> {
                                    val lastIsError = last.value.data?.user?.importedMailCategoryFilters == null
                                    if (lastIsError && viewModelState.apolloResponseStates.size <= 1) {
                                        SettingMailCategoryFilterScreenUiState.LoadingState.Error
                                    } else {
                                        SettingMailCategoryFilterScreenUiState.LoadingState.Loaded(
                                            filters =
                                            viewModelState.apolloResponseStates.mapNotNull { items ->
                                                when (items) {
                                                    is ApolloResponseState.Failure -> null
                                                    is ApolloResponseState.Loading -> null
                                                    is ApolloResponseState.Success -> items.value
                                                }
                                            }.map { apolloResponse ->
                                                apolloResponse.data?.user?.importedMailCategoryFilters?.nodes.orEmpty()
                                                    .map { item ->
                                                        SettingMailCategoryFilterScreenUiState.Item(
                                                            title = item.title,
                                                            event =
                                                            object : SettingMailCategoryFilterScreenUiState.ItemEvent {
                                                                override fun onClick() {
                                                                    viewModelScope.launch {
                                                                        eventSender.send {
                                                                            it.navigate(
                                                                                ScreenStructure.Root.Settings.MailCategoryFilter(
                                                                                    id = item.id,
                                                                                ),
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                        )
                                                    }
                                            }.flatten().toImmutableList(),
                                            isError = lastIsError,
                                            event = loadedEvent,
                                        )
                                    }
                                }
                            }

                        uiState.copy(
                            loadingState = loadingState,
                            textInput = viewModelState.textInputDialog,
                        )
                    }
                }
            }
        }.asStateFlow()

    init {
        viewModelScope.launch {
            pagingModel.getFlow().collectLatest { apolloResponseStates ->
                viewModelStateFlow.update {
                    it.copy(
                        apolloResponseStates = apolloResponseStates,
                    )
                }
            }
        }
    }

    public interface Event {
        public fun navigate(structure: ScreenStructure)
    }

    private data class ViewModelState(
        val apolloResponseStates: List<ApolloResponseState<ApolloResponse<ImportedMailCategoryFiltersScreenPagingQuery.Data>>> = listOf(),
        val textInputDialog: SettingMailCategoryFilterScreenUiState.TextInput? = null,
    )
}
