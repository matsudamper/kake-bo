package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab

public data class RootHomeMonthlyCategoryScreenUiState(
    val loadingState: LoadingState,
    val title: String,
    val event: Event,
) {
    public sealed interface LoadingState {
        public data object Loaded : LoadingState
        public data object Loading : LoadingState
        public data object Error : LoadingState
    }
    public interface Event {
        public fun onViewInitialized()
    }
}

@Composable
public fun RootHomeMonthlyCategoryScreen(
    uiState: RootHomeMonthlyCategoryScreenUiState,
    modifier: Modifier = Modifier,
    scaffoldListener: RootScreenScaffoldListener,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    RootScreenScaffold(
        currentScreen = RootScreenTab.Home,
        listener = scaffoldListener,
        topBar = {
            KakeBoTopAppBar {
                Text(uiState.title)
            }
        },
    ) {
        when (val loadingState = uiState.loadingState) {
            is RootHomeMonthlyCategoryScreenUiState.LoadingState.Loaded -> {
                Text("TODO: $loadingState")
            }

            RootHomeMonthlyCategoryScreenUiState.LoadingState.Loading -> {
                Box(modifier = modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            RootHomeMonthlyCategoryScreenUiState.LoadingState.Error -> {
                LoadingErrorContent(
                    modifier = modifier,
                    onClickRetry = { /* TODO */ },
                )
            }
        }
    }
}