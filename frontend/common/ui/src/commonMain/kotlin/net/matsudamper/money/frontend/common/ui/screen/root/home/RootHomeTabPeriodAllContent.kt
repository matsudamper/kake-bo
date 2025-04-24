package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.LocalIsLargeScreen
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.layout.graph.bar.BarGraph
import net.matsudamper.money.frontend.common.ui.layout.graph.bar.BarGraphUiState

public data class RootHomeTabPeriodAllContentUiState(
    val rootHomeTabUiState: RootHomeTabScreenScaffoldUiState,
    val loadingState: LoadingState,
    val rootHomeTabPeriodUiState: RootHomeTabPeriodUiState,
    val scaffoldListener: RootScreenScaffoldListener,
    val event: Event,
) {
    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data object Error : LoadingState

        public data class Loaded(
            val barGraph: BarGraphUiState,
            val monthTotalItems: ImmutableList<RootHomeTabPeriodUiState.MonthTotalItem>,
            val totalBarColorTextMapping: ImmutableList<GraphTitleChipUiState>,
        ) : LoadingState
    }

    public interface Event {
        public suspend fun onViewInitialized()
        public fun refresh()
    }
}

@Composable
public fun RootHomeTabPeriodAllScreen(
    modifier: Modifier = Modifier,
    uiState: RootHomeTabPeriodAllContentUiState,
    contentPadding: PaddingValues,
) {
    RootHomeTabPeriodScaffold(
        modifier = modifier.fillMaxSize(),
        uiState = uiState.rootHomeTabPeriodUiState,
        homeUiState = uiState.rootHomeTabUiState,
        scaffoldListener = uiState.scaffoldListener,
        windowInsets = contentPadding,
        onRefresh = { uiState.event.refresh() },
    ) {
        when (val loadingState = uiState.loadingState) {
            is RootHomeTabPeriodAllContentUiState.LoadingState.Loaded -> {
                if (LocalIsLargeScreen.current) {
                    LargeContent(
                        loadingState = loadingState,
                        modifier = Modifier,
                    )
                } else {
                    SmallContent(
                        loadingState = loadingState,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            RootHomeTabPeriodAllContentUiState.LoadingState.Loading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            RootHomeTabPeriodAllContentUiState.LoadingState.Error -> {
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
private fun SmallContent(
    loadingState: RootHomeTabPeriodAllContentUiState.LoadingState.Loaded,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Card(modifier = Modifier) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                BarGraph(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(600.dp),
                    uiState = loadingState.barGraph,
                    contentColor = LocalContentColor.current,
                )
                Spacer(modifier = Modifier.height(12.dp))
                GraphTitleChips(
                    modifier = Modifier,
                    items = loadingState.totalBarColorTextMapping,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = modifier
                .width(intrinsicSize = IntrinsicSize.Min),
        ) {
            MonthlyTotal(
                loadingState = loadingState,
                modifier = Modifier.fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun LargeContent(
    loadingState: RootHomeTabPeriodAllContentUiState.LoadingState.Loaded,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.widthIn(max = 1200.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
        ) {
            BarGraph(
                modifier = Modifier
                    .weight(1f)
                    .height(600.dp),
                uiState = loadingState.barGraph,
                contentColor = LocalContentColor.current,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .widthIn(max = 400.dp),
            ) {
                MonthlyTotal(
                    modifier = Modifier.fillMaxWidth(),
                    loadingState = loadingState,
                )
                Spacer(modifier = Modifier.height(12.dp))
                GraphTitleChips(
                    modifier = Modifier.fillMaxWidth(),
                    items = loadingState.totalBarColorTextMapping,
                )
            }
        }
    }
}

@Composable
private fun MonthlyTotal(
    loadingState: RootHomeTabPeriodAllContentUiState.LoadingState.Loaded,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        loadingState.monthTotalItems.forEach { item ->
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { item.event.onClick() }
                    .padding(8.dp),
            ) {
                Text(
                    text = item.title,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.widthIn(min = 16.dp).weight(1f))
                Text(
                    text = item.amount,
                    maxLines = 1,
                )
            }
        }
    }
}
