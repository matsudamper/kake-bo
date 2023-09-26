package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.layout.graph.BarGraph
import net.matsudamper.money.frontend.common.ui.layout.graph.BarGraphUiState

public data class RootHomeTabPeriodAllContentUiState(
    val rootHomeTabUiState: RootHomeTabScreenScaffoldUiState,
    val loadingState: LoadingState,
    val rootHomeTabPeriodUiState: RootHomeTabPeriodUiState,
) {
    public sealed interface LoadingState {
        public data object Loading : LoadingState
        public data object Error : LoadingState
        public data class Loaded(
            val barGraph: BarGraphUiState,
            val monthTotalItems: ImmutableList<RootHomeTabPeriodUiState.MonthTotalItem>,
            val totalBarColorTextMapping: ImmutableList<RootHomeTabScreenScaffoldUiState.ColorText>,
        ) : LoadingState
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
public fun RootHomeTabPeriodAllScreen(
    modifier: Modifier = Modifier,
    uiState: RootHomeTabPeriodAllContentUiState,
    scaffoldListener: RootScreenScaffoldListener,
) {
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
                    Card(modifier = modifier) {
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
                            FlowRow(modifier = Modifier.fillMaxWidth()) {
                                loadingState.totalBarColorTextMapping.forEach {
                                    AssistChip(
                                        onClick = {
                                            it.onClick()
                                        },
                                        label = {
                                            Row(
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(it.color),
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(it.text)
                                            }
                                        },
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                            }
                        }
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
