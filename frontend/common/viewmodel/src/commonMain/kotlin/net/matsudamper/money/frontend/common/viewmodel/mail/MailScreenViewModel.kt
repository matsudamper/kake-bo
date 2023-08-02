package net.matsudamper.money.frontend.common.viewmodel.mail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.matsudamper.money.frontend.common.ui.screen.mail.MailScreenUiState

public class MailScreenViewModel(
    private val coroutineScope: CoroutineScope,
) {

    public val uiStateFlow: StateFlow<MailScreenUiState> = MutableStateFlow(
        MailScreenUiState(
            event = object : MailScreenUiState.Event {

            },
        ),
    ).also { uiStateFlow ->

    }.asStateFlow()
}