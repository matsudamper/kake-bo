package net.matsudamper.money.frontend.common.ui.screen.root.home.monthly

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabScreenScaffold
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabScreenScaffoldUiState

public data class RootHomeMonthlyScreenUiState(
    val loadingState: LoadingState,
    val rootHomeTabUiState: RootHomeTabScreenScaffoldUiState,
    val event: Event,
) {
    public sealed interface LoadingState {
        public data object Loading : LoadingState
        public data object Error : LoadingState
        public data class Loaded(
            val item: Any,
        ) : LoadingState
    }

    @Immutable
    public interface Event {
        public suspend fun onViewInitialized()
    }
}

@Composable
public fun RootHomeMonthlyScreen(
    uiState: RootHomeMonthlyScreenUiState,
    scaffoldListener: RootScreenScaffoldListener,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    RootHomeTabScreenScaffold(
        modifier = modifier,
        uiState = uiState.rootHomeTabUiState,
        scaffoldListener = scaffoldListener,
    ) {
        when (val loadingState = uiState.loadingState) {
            is RootHomeMonthlyScreenUiState.LoadingState.Loaded -> {
                LoadedContent(
                    modifier = Modifier.fillMaxSize(),
                    loadingState = loadingState,
                )
            }

            RootHomeMonthlyScreenUiState.LoadingState.Loading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            RootHomeMonthlyScreenUiState.LoadingState.Error -> {
                LoadingErrorContent(
                    modifier = Modifier.fillMaxWidth(),
                    onClickRetry = {
                        // TODO
                    },
                )
            }
        }
    }
}

@Composable
private fun LoadedContent(
    loadingState: RootHomeMonthlyScreenUiState.LoadingState.Loaded,
    modifier: Modifier = Modifier,
) {

}
