package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.layout.graph.BarGraph
import net.matsudamper.money.frontend.common.ui.layout.graph.BarGraphUiState

public data class RootHomeTabPeriodCategoryContentUiState(
    val loadingState: LoadingState,
    val rootHomeTabUiState: RootHomeTabScreenScaffoldUiState,
    val rootHomeTabPeriodUiState: RootHomeTabPeriodUiState,
    val event: Event,
) {

    public sealed interface LoadingState {
        public data object Error : LoadingState
        public data object Loading : LoadingState
        public data class Loaded(
            val graphItems: BarGraphUiState,
            val graphTitleItems: ImmutableList<GraphTitleChipUiState>,
            val monthTotalItems: ImmutableList<RootHomeTabPeriodUiState.MonthTotalItem>,
        ) : LoadingState
    }

    public interface Event {
        public suspend fun onViewInitialized()
    }
}

@Composable
public fun RootHomeTabPeriodCategoryScreen(
    modifier: Modifier = Modifier,
    uiState: RootHomeTabPeriodCategoryContentUiState,
    scaffoldListener: RootScreenScaffoldListener,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    RootHomeTabScreenScaffold(
        uiState = uiState.rootHomeTabUiState,
        scaffoldListener = scaffoldListener,
    ) {
        RootHomeTabPeriodScaffold(
            modifier = Modifier.fillMaxSize(),
            uiState = uiState.rootHomeTabPeriodUiState,
        ) {
            when (val loadingState = uiState.loadingState) {
                is RootHomeTabPeriodCategoryContentUiState.LoadingState.Loaded -> {
                    LoadedContent(
                        modifier = Modifier.fillMaxSize(),
                        loadingState = loadingState,
                    )
                }

                RootHomeTabPeriodCategoryContentUiState.LoadingState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }

                RootHomeTabPeriodCategoryContentUiState.LoadingState.Error -> {
                    LoadingErrorContent(
                        modifier = modifier,
                        onClickRetry = { /* TODO */ },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadedContent(
    modifier: Modifier = Modifier,
    loadingState: RootHomeTabPeriodCategoryContentUiState.LoadingState.Loaded,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(12.dp))
        Card {
            BarGraph(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(500.dp),
                uiState = loadingState.graphItems,
                contentColor = LocalContentColor.current,
            )
            Spacer(modifier = Modifier.height(8.dp))
            GraphTitleChips(
                modifier = Modifier.padding(horizontal = 16.dp),
                items = loadingState.graphTitleItems,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                loadingState.monthTotalItems.forEach {
                    Row {
                        Text(it.title)
                        Spacer(modifier = Modifier.widthIn(min = 8.dp).weight(1f))
                        Text(it.amount)
                    }
                }
            }
        }
    }
}
