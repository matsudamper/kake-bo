package net.matsudamper.money.frontend.common.viewmodel.root.mail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.mail.MailScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender

public class MailScreenViewModel(
    private val coroutineScope: CoroutineScope,
) {
    private val _viewModelStateFlow = MutableStateFlow(ViewModelState())
    public val viewModelStateFlow: StateFlow<ViewModelState> = _viewModelStateFlow.asStateFlow()

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
        ),
    )

    public fun updateScreenStructure(structure: ScreenStructure.Root.Mail) {
        _viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                screenStructure = structure,
                lastImportedMailStructure = (structure as? ScreenStructure.Root.Mail.Imported)
                    ?: viewModelState.lastImportedMailStructure,
                lastImportMailStructure = (structure as? ScreenStructure.Root.Mail.Import)
                    ?: viewModelState.lastImportMailStructure,
            )
        }
    }

    public interface Event {
        public fun navigateToImportMail()
        public fun navigateToImportedMail()
    }

    public data class ViewModelState(
        val screenStructure: ScreenStructure.Root.Mail? = null,
        val lastImportedMailStructure: ScreenStructure.Root.Mail.Imported? = null,
        val lastImportMailStructure: ScreenStructure.Root.Mail.Import? = null,
    )
}
