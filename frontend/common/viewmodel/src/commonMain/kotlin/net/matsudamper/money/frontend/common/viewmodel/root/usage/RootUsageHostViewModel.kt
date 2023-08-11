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

    private val eventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = eventSender.asHandler()

    public val uiStateFlow: StateFlow<RootUsageHostScreenUiState> = MutableStateFlow(
        RootUsageHostScreenUiState(
            type = RootUsageHostScreenUiState.Type.Calendar,
            event = object : RootUsageHostScreenUiState.Event {
                override fun onViewInitialized() {
                    // TODO
                }

                override fun onClickCalendar() {
                    coroutineScope.launch {
                        eventSender.send {
                            it.navigate(ScreenStructure.Root.Usage.Calendar())
                        }
                    }
                    viewModelStateFlow.update {
                        it.copy(type = RootUsageHostScreenUiState.Type.Calendar)
                    }
                }

                override fun onClickList() {
                    coroutineScope.launch {
                        eventSender.send {
                            it.navigate(ScreenStructure.Root.Usage.List())
                        }
                    }
                    viewModelStateFlow.update {
                        it.copy(type = RootUsageHostScreenUiState.Type.List)
                    }
                }
            },
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow
                .collectLatest { viewModelState ->

                    uiStateFlow.update { uiState ->
                        uiState.copy(
                            type = viewModelState.type,
                        )
                    }
                }
        }
    }.asStateFlow()

    init {
        coroutineScope.launch {
        }
    }

    public interface Event {
        public fun navigate(screenStructure: ScreenStructure)
    }

    private data class ViewModelState(
        val type: RootUsageHostScreenUiState.Type = RootUsageHostScreenUiState.Type.Calendar,
    )
}
