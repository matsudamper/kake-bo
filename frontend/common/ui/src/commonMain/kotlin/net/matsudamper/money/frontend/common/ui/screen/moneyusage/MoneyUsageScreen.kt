package net.matsudamper.money.frontend.common.ui.screen.moneyusage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffold
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener

public data class MoneyUsageScreenUiState(
    val event: Event,
    val loadingState: LoadingState,
) {
    @Immutable
    public sealed interface LoadingState {
        public object Loading : LoadingState
        public data class Loaded(
            val items: List<Item>,
        ) : LoadingState
    }

    public data class Item(
        val title: String,
    )

    @Immutable
    public interface Event
}

@Composable
public fun MoneyUsageScreen(
    modifier: Modifier = Modifier,
    uiState: MoneyUsageScreenUiState,
    kakeboScaffoldListener: KakeboScaffoldListener,
) {
    KakeboScaffold(
        modifier = modifier,
        listener = kakeboScaffoldListener,
    ) {

    }
}
