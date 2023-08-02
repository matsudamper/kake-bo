package net.matsudamper.money.frontend.common.ui.screen.mail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier

public data class MailScreenUiState(
    val event: Event,
) {
    @Immutable
    public interface Event {

    }
}

@Composable
public fun MailScreen(
    modifier: Modifier = Modifier,
    uiState: MailScreenUiState,
) {
}