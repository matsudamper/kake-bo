package net.matsudamper.money.frontend.common.viewmodel.root.usage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageHostScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender

public class RootUsageHostViewModel(
    private val coroutineScope: CoroutineScope,
    public val calendarPagingModel: RootUsageCalendarPagingModel,
) {
    private val mutableViewModelStateFlow = MutableStateFlow(ViewModelState())
    public val viewModelStateFlow: StateFlow<ViewModelState> = mutableViewModelStateFlow.asStateFlow()

    private val rootNavigationEventSender = EventSender<RootNavigationEvent>()
    public val rootNavigationEventHandler: EventHandler<RootNavigationEvent> = rootNavigationEventSender.asHandler()
    private val event =
        object : RootUsageHostScreenUiState.Event {
            override suspend fun onViewInitialized() {
            }

            override fun onClickAdd() {
                coroutineScope.launch {
                    rootNavigationEventSender.send {
                        it.navigate(ScreenStructure.AddMoneyUsage())
                    }
                }
            }

            override fun onClickCalendar() {
                coroutineScope.launch {
                    rootNavigationEventSender.send {
                        it.navigate(ScreenStructure.Root.Usage.Calendar())
                    }
                }
            }

            override fun onClickList() {
                coroutineScope.launch {
                    rootNavigationEventSender.send {
                        it.navigate(ScreenStructure.Root.Usage.List())
                    }
                }
            }

            override fun onClickSearchBox() {
                mutableViewModelStateFlow.update {
                    it.copy(
                        textInputUiState =
                        RootUsageHostScreenUiState.TextInputUiState(
                            title = "検索",
                            default = mutableViewModelStateFlow.value.searchText,
                            inputType = "text",
                            textComplete = { text ->
                                updateSearchText(text)
                                closeTextInput()
                            },
                            canceled = {
                                closeTextInput()
                            },
                            isMultiline = false,
                            name = "",
                        ),
                    )
                }
            }

            override fun onClickSearchBoxClear() {
                updateSearchText("")
            }

            private fun closeTextInput() {
                mutableViewModelStateFlow.update {
                    it.copy(
                        textInputUiState = null,
                    )
                }
            }

            private fun updateSearchText(text: String) {
                mutableViewModelStateFlow.update {
                    it.copy(
                        searchText = text,
                    )
                }
            }
        }

    public val uiStateFlow: StateFlow<RootUsageHostScreenUiState> =
        MutableStateFlow(
            RootUsageHostScreenUiState(
                type = RootUsageHostScreenUiState.Type.Calendar,
                header = RootUsageHostScreenUiState.Header.None,
                textInputUiState = null,
                searchText = "",
                event = event,
            ),
        ).also { uiStateFlow ->
            coroutineScope.launch {
                mutableViewModelStateFlow
                    .collectLatest { viewModelState ->
                        viewModelState.screenStructure ?: return@collectLatest

                        uiStateFlow.update {
                            it.copy(
                                type =
                                when (viewModelState.screenStructure) {
                                    is ScreenStructure.Root.Usage.Calendar -> RootUsageHostScreenUiState.Type.Calendar
                                    is ScreenStructure.Root.Usage.List -> RootUsageHostScreenUiState.Type.List
                                },
                                header =
                                when (viewModelState.screenStructure) {
                                    is ScreenStructure.Root.Usage.Calendar -> viewModelState.calendarHeader
                                    is ScreenStructure.Root.Usage.List -> RootUsageHostScreenUiState.Header.None
                                } ?: RootUsageHostScreenUiState.Header.None,
                                textInputUiState = viewModelState.textInputUiState,
                                searchText = viewModelState.searchText,
                            )
                        }
                    }
            }
        }.asStateFlow()

    public fun updateHeader(calendarHeader: RootUsageHostScreenUiState.Header.Calendar?) {
        mutableViewModelStateFlow.update {
            it.copy(
                calendarHeader = calendarHeader,
            )
        }
    }

    public fun updateStructure(structure: ScreenStructure.Root.Usage) {
        mutableViewModelStateFlow.update {
            it.copy(
                screenStructure = structure,
            )
        }
    }

    public fun requestNavigate() {
        coroutineScope.launch {
            rootNavigationEventSender.send {
                it.navigate(
                    mutableViewModelStateFlow.value.screenStructure
                        ?: ScreenStructure.Root.Usage.Calendar(),
                )
            }
        }
    }

    public interface RootNavigationEvent {
        public fun navigate(screenStructure: ScreenStructure)
    }

    public data class ViewModelState(
        val screenStructure: ScreenStructure.Root.Usage? = null,
        val calendarHeader: RootUsageHostScreenUiState.Header.Calendar? = null,
        val textInputUiState: RootUsageHostScreenUiState.TextInputUiState? = null,
        val searchText: String = "",
    )
}
