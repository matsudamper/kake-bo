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
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.settings.SettingMailCategoryFilterScreenUiState
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EqualsImpl
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFiltersScreenPagingQuery
import net.matsudamper.money.frontend.graphql.UpdateOperationResponseResult

public class SettingMailCategoryFiltersViewModel(
    private val pagingModel: ImportedMailCategoryFilterScreenPagingModel,
    private val api: SettingImportedMailCategoryFilterApi,
    scopedObjectFeature: ScopedObjectFeature,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(ViewModelState())

    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val loadedEvent = object : SettingMailCategoryFilterScreenUiState.LoadedEvent {
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
                            listFetch()
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

    public val uiStateFlow: StateFlow<SettingMailCategoryFilterScreenUiState> = MutableStateFlow(
        SettingMailCategoryFilterScreenUiState(
            loadingState = SettingMailCategoryFilterScreenUiState.LoadingState.Loading,
            textInput = null,
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navController.navigateToHome()
                }
            },
            event = object : SettingMailCategoryFilterScreenUiState.Event {
                override fun onClickRetry() {
                    viewModelScope.launch {
                        listFetch()
                    }
                }

                override fun onViewInitialized() {
                    viewModelScope.launch {
                        listFetch()
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
                    val loadingState = if (viewModelState.apolloResponseStates == null && viewModelState.isLoading) {
                        SettingMailCategoryFilterScreenUiState.LoadingState.Loading
                    } else {
                        SettingMailCategoryFilterScreenUiState.LoadingState.Loaded(
                            filters = viewModelState.apolloResponseStates?.data?.user?.importedMailCategoryFilters?.nodes.orEmpty().map { item ->
                                SettingMailCategoryFilterScreenUiState.Item(
                                    title = item.title,
                                    event = ItemEventListener(item),
                                )
                            }.toImmutableList(),
                            isError = when (val it = viewModelState.lastLoadingState) {
                                is UpdateOperationResponseResult.Error -> true
                                is UpdateOperationResponseResult.NoHasMore -> false
                                null -> false
                                is UpdateOperationResponseResult.Success -> {
                                    it.result.data?.user?.importedMailCategoryFilters != null
                                }
                            },
                            event = loadedEvent,
                        )
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

    private fun listFetch() {
        viewModelScope.launch {
            val result = pagingModel.fetch()
            viewModelStateFlow.update {
                it.copy(
                    lastLoadingState = result,
                )
            }
        }
    }

    public interface Event {
        public fun navigate(structure: ScreenStructure)
    }

    private inner class ItemEventListener(
        private val item: ImportedMailCategoryFiltersScreenPagingQuery.Node,
    ) : SettingMailCategoryFilterScreenUiState.ItemEvent, EqualsImpl(item) {
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
    }

    private data class ViewModelState(
        val apolloResponseStates: ApolloResponse<ImportedMailCategoryFiltersScreenPagingQuery.Data>? = null,
        val lastLoadingState: UpdateOperationResponseResult<ImportedMailCategoryFiltersScreenPagingQuery.Data>? = null,
        val textInputDialog: SettingMailCategoryFilterScreenUiState.TextInput? = null,
        val isLoading: Boolean = true,
    )
}
