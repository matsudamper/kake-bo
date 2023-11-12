package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.layout.graph.bar.BarGraph
import net.matsudamper.money.frontend.common.ui.layout.graph.bar.BarGraphUiState

public data class RootHomeTabPeriodAllContentUiState(
    val rootHomeTabUiState: RootHomeTabScreenScaffoldUiState,
    val loadingState: LoadingState,
    val rootHomeTabPeriodUiState: RootHomeTabPeriodUiState,
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
    }
}

@Composable
public fun RootHomeTabPeriodAllScreen(
    modifier: Modifier = Modifier,
    uiState: RootHomeTabPeriodAllContentUiState,
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
                is RootHomeTabPeriodAllContentUiState.LoadingState.Loaded -> {
                    BoxWithConstraints {
                        val width by rememberUpdatedState(maxWidth)
                        if (width > 800.dp) {
                            Row {
                                GraphCard(
                                    loadingState = loadingState,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                MonthlyTotalCard(
                                    loadingState = loadingState,
                                    modifier = Modifier,
                                )
                            }
                        } else {
                            Column {
                                GraphCard(
                                    loadingState = loadingState,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                MonthlyTotalCard(
                                    loadingState = loadingState,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
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
}

@Composable
private fun GraphCard(
    loadingState: RootHomeTabPeriodAllContentUiState.LoadingState.Loaded,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            BarGraph(
                modifier = Modifier
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
}

@Composable
private fun MonthlyTotalCard(
    loadingState: RootHomeTabPeriodAllContentUiState.LoadingState.Loaded,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.width(intrinsicSize = IntrinsicSize.Min)) {
        Column(modifier = Modifier.padding(16.dp)) {
            loadingState.monthTotalItems.forEach {
                Row {
                    Text(
                        text = it.title,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.widthIn(min = 16.dp).weight(1f))
                    Text(
                        text = it.amount,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
