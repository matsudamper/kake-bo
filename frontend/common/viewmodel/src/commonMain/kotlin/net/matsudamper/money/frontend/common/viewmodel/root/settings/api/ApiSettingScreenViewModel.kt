package net.matsudamper.money.frontend.common.viewmodel.root.settings.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.settings.ApiSettingScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender

public class ApiSettingScreenViewModel(
    private val coroutineScope: CoroutineScope,
) {
    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    public val uiStateFlow: StateFlow<ApiSettingScreenUiState> = MutableStateFlow(
        ApiSettingScreenUiState(
            event = object : ApiSettingScreenUiState.Event {
                override fun onClickBack() {
                    coroutineScope.launch {
                        eventSender.send {
                            it.navigate(ScreenStructure.Root.Settings.Root)
                        }
                    }
                }
            },
        ),
    )

    public interface Event {
        public fun navigate(structure: ScreenStructure)
    }
}