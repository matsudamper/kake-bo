package net.matsudamper.money.frontend.common.viewmodel.root.usage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.ApolloClient
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageHostScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.GraphqlClient

public class RootUsageHostViewModel(
    private val coroutineScope: CoroutineScope,
    apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val calendarViewModel: RootUsageCalendarViewModel = RootUsageCalendarViewModel(
        coroutineScope = coroutineScope,
    )
    public val listViewModel: RootUsageListViewModel = RootUsageListViewModel(
        coroutineScope = coroutineScope,
    )

    private val rootNavigationEventSender = EventSender<RootNavigationEvent>()
    public val rootNavigationEventHandler: EventHandler<RootNavigationEvent> = rootNavigationEventSender.asHandler()

    public val uiStateFlow: StateFlow<RootUsageHostScreenUiState> = MutableStateFlow(
        RootUsageHostScreenUiState(
            type = RootUsageHostScreenUiState.Type.Calendar,
            event = object : RootUsageHostScreenUiState.Event {
                override fun onViewInitialized() {
                    // TODO
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
            },
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow
                .collectLatest { viewModelState ->
                    viewModelState.screenStructure ?: return@collectLatest

                    uiStateFlow.update {
                        it.copy(
                            type = when (viewModelState.screenStructure) {
                                is ScreenStructure.Root.Usage.Calendar -> RootUsageHostScreenUiState.Type.Calendar
                                is ScreenStructure.Root.Usage.List -> RootUsageHostScreenUiState.Type.List
                            }
                        )
                    }
                }
        }
    }.asStateFlow()

    init {
        coroutineScope.launch {
        }
    }

    public fun updateStructure(structure: ScreenStructure.Root.Usage) {
        viewModelStateFlow.update {
            it.copy(
                screenStructure = structure,
            )
        }
    }

    public fun requestNavigate() {
        coroutineScope.launch {
            rootNavigationEventSender.send {
                it.navigate(
                    viewModelStateFlow.value.screenStructure
                        ?: ScreenStructure.Root.Usage.Calendar(),
                )
            }
        }
    }

    public interface RootNavigationEvent {
        public fun navigate(screenStructure: ScreenStructure)
    }


    private data class ViewModelState(
        val screenStructure: ScreenStructure.Root.Usage? = null,
    )
}
