package net.matsudamper.money.frontend.common.viewmodel.root.mail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.ui.screen.mail.MailScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender

public class MailScreenViewModel(
    private val coroutineScope: CoroutineScope,
) {
    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    public val uiStateFlow: StateFlow<MailScreenUiState> = MutableStateFlow(
        MailScreenUiState(
            event = object : MailScreenUiState.Event {
                override fun onClickImportTabButton() {
                    coroutineScope.launch {
                        eventSender.send {
                            it.navigateToImportMail()
                        }
                    }
                }

                override fun onClickImportedTabButton() {
                    coroutineScope.launch {
                        eventSender.send {
                            it.navigateToImportedMail()
                        }
                    }
                }
            },
        )
    )

    public interface Event {
        public fun navigateToImportMail()
        public fun navigateToImportedMail()
    }
}
