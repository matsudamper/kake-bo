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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.ui.LocalIsLargeScreen
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.layout.graph.bar.BarGraph
import net.matsudamper.money.frontend.common.ui.layout.graph.bar.BarGraphUiState
import org.jetbrains.compose.ui.tooling.preview.Preview

public data class RootHomeTabPeriodAllContentUiState(
    val loadingState: LoadingState,
    val rootHomeTabPeriodAndCategoryUiState: RootHomeTabPeriodAndCategoryUiState,
    val kakeboScaffoldListener: KakeboScaffoldListener,
    val event: Event,
) {
    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data object Error : LoadingState

        public data class Loaded(
            val barGraph: BarGraphUiState,
            val monthTotalItems: ImmutableList<RootHomeTabPeriodAndCategoryUiState.MonthTotalItem>,
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
        uiState = uiState.rootHomeTabPeriodAndCategoryUiState,
        kakeboScaffoldListener = uiState.kakeboScaffoldListener,
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
@Preview
private fun PeriodAnalyticsScreenPreview() {
    val noOpBarEvent = object : BarGraphUiState.PeriodDataEvent {
        override fun onClick() {}
    }
    val noOpMonthEvent = object : RootHomeTabPeriodAndCategoryUiState.MonthTotalItem.Event {
        override fun onClick() {}
    }
    AppRoot(isDarkTheme = false) {
        RootHomeTabPeriodAllScreen(
            modifier = Modifier.fillMaxSize(),
            uiState = RootHomeTabPeriodAllContentUiState(
                loadingState = RootHomeTabPeriodAllContentUiState.LoadingState.Loaded(
                    barGraph = BarGraphUiState(
                        items = listOf(
                            BarGraphUiState.PeriodData(
                                year = 2025,
                                month = 12,
                                items = listOf(
                                    BarGraphUiState.Item(color = Color(0xFF558B2F), title = "食費", value = 45000),
                                    BarGraphUiState.Item(color = Color(0xFF1565C0), title = "光熱費", value = 20000),
                                    BarGraphUiState.Item(color = Color(0xFFE65100), title = "ショッピング", value = 30000),
                                ).toImmutableList(),
                                total = 95000,
                                event = noOpBarEvent,
                            ),
                            BarGraphUiState.PeriodData(
                                year = 2026,
                                month = 1,
                                items = listOf(
                                    BarGraphUiState.Item(color = Color(0xFF558B2F), title = "食費", value = 52000),
                                    BarGraphUiState.Item(color = Color(0xFF1565C0), title = "光熱費", value = 25000),
                                    BarGraphUiState.Item(color = Color(0xFFE65100), title = "ショッピング", value = 18000),
                                ).toImmutableList(),
                                total = 95000,
                                event = noOpBarEvent,
                            ),
                            BarGraphUiState.PeriodData(
                                year = 2026,
                                month = 2,
                                items = listOf(
                                    BarGraphUiState.Item(color = Color(0xFF558B2F), title = "食費", value = 48000),
                                    BarGraphUiState.Item(color = Color(0xFF1565C0), title = "光熱費", value = 22000),
                                    BarGraphUiState.Item(color = Color(0xFFE65100), title = "ショッピング", value = 35000),
                                ).toImmutableList(),
                                total = 105000,
                                event = noOpBarEvent,
                            ),
                        ).toImmutableList(),
                    ),
                    monthTotalItems = listOf(
                        RootHomeTabPeriodAndCategoryUiState.MonthTotalItem(
                            title = "2025/12",
                            amount = "¥95,000",
                            event = noOpMonthEvent,
                        ),
                        RootHomeTabPeriodAndCategoryUiState.MonthTotalItem(
                            title = "2026/01",
                            amount = "¥95,000",
                            event = noOpMonthEvent,
                        ),
                        RootHomeTabPeriodAndCategoryUiState.MonthTotalItem(
                            title = "2026/02",
                            amount = "¥105,000",
                            event = noOpMonthEvent,
                        ),
                    ).toImmutableList(),
                    totalBarColorTextMapping = listOf(
                        GraphTitleChipUiState(title = "食費", color = Color(0xFF558B2F), onClick = {}),
                        GraphTitleChipUiState(title = "光熱費", color = Color(0xFF1565C0), onClick = {}),
                        GraphTitleChipUiState(title = "ショッピング", color = Color(0xFFE65100), onClick = {}),
                    ).toImmutableList(),
                ),
                rootHomeTabPeriodAndCategoryUiState = RootHomeTabPeriodAndCategoryUiState(
                    loadingState = RootHomeTabPeriodAndCategoryUiState.LoadingState.Loaded(
                        rangeText = "3ヶ月",
                        between = "2025-12 ~ 2026-02",
                        categoryType = "すべて",
                        categoryTypes = listOf(
                            RootHomeTabPeriodAndCategoryUiState.CategoryTypes(title = "すべて", onClick = {}),
                        ).toImmutableList(),
                    ),
                    event = object : RootHomeTabPeriodAndCategoryUiState.Event {
                        override fun onClickNextMonth() {}
                        override fun onClickPreviousMonth() {}
                        override fun onClickRange(range: Int) {}
                        override fun onViewInitialized() {}
                        override fun onClickRetry() {}
                    },
                ),
                kakeboScaffoldListener = object : KakeboScaffoldListener {
                    override fun onClickTitle() {}
                },
                event = object : RootHomeTabPeriodAllContentUiState.Event {
                    override suspend fun onViewInitialized() {}
                    override fun refresh() {}
                },
            ),
            contentPadding = PaddingValues(),
        )
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
