package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.ScrollButtons
import net.matsudamper.money.frontend.common.ui.ScrollButtonsDefaults
import net.matsudamper.money.frontend.common.ui.base.DropDownMenuButton
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.layout.graph.BarGraph
import net.matsudamper.money.frontend.common.ui.layout.graph.BarGraphUiState
import net.matsudamper.money.frontend.common.ui.layout.graph.PolygonalLineGraph
import net.matsudamper.money.frontend.common.ui.layout.graph.PolygonalLineGraphItemUiState

public data class RootHomeTabPeriodContentUiState(
    val loadingState: LoadingState,
    val event: Event,
) {
    public sealed interface LoadingState {

        public data class Loaded(
            val rangeText: String,
            val between: String,
            val categoryType: String,
            val categoryTypes: ImmutableList<CategoryTypes>,
            val graphContent: GraphContent,
        ) : LoadingState

        public object Loading : LoadingState
        public object Error : LoadingState
    }

    public data class CategoryTypes(
        val title: String,
        val onClick: () -> Unit,
    )

    public sealed interface GraphContent {
        public data class Total(
            val barGraph: BarGraphUiState,
            val monthTotalItems: ImmutableList<MonthTotalItem>,
            val totalBarColorTextMapping: ImmutableList<RootHomeTabUiState.ColorText>,
        ) : GraphContent

        public data class Category(
            val graphItems: ImmutableList<PolygonalLineGraphItemUiState>,
            val monthTotalItems: ImmutableList<MonthTotalItem>,
        ) : GraphContent
    }

    public data class MonthTotalItem(
        val title: String,
        val amount: String,
    )

    @Immutable
    public interface Event {
        public fun onClickNextMonth()
        public fun onClickPreviousMonth()
        public fun onClickRange(range: Int)
        public fun onViewInitialized()
        public fun onClickRetry()
    }
}

@Composable
public fun RootHomeTabPeriodContent(
    modifier: Modifier = Modifier,
    uiState: RootHomeTabPeriodContentUiState,
) {
    LaunchedEffect(uiState.event) {
        uiState.event.onViewInitialized()
    }
    BoxWithConstraints(modifier = modifier) {
        when (uiState.loadingState) {
            RootHomeTabPeriodContentUiState.LoadingState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            RootHomeTabPeriodContentUiState.LoadingState.Error -> {
                LoadingErrorContent(
                    modifier = Modifier.fillMaxWidth(),
                    onClickRetry = {
                        uiState.event.onClickRetry()
                    },
                )
            }

            is RootHomeTabPeriodContentUiState.LoadingState.Loaded -> {
                val height = this.maxHeight
                val scrollState = rememberScrollState()
                val density = LocalDensity.current
                var scrollBarHeight by remember { mutableStateOf(0) }
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp),
                ) {
                    PeriodSection(
                        modifier = Modifier.fillMaxWidth(),
                        onClickPreviousMonth = { uiState.event.onClickPreviousMonth() },
                        onClickNextMonth = { uiState.event.onClickNextMonth() },
                        betweenText = {
                            Text(uiState.loadingState.between)
                        },
                        rangeText = {
                            Text(uiState.loadingState.rangeText)
                        },
                        onClickRange = { range -> uiState.event.onClickRange(range) },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    BetweenLoaded(
                        uiState = uiState.loadingState,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Spacer(modifier = Modifier.height(with(density) { scrollBarHeight.toDp() }))
                }
                ScrollButtons(
                    modifier = Modifier
                        .onSizeChanged {
                            scrollBarHeight = it.height
                        }
                        .align(Alignment.BottomEnd)
                        .padding(ScrollButtonsDefaults.padding)
                        .height(ScrollButtonsDefaults.height),
                    scrollState = scrollState,
                    scrollSize = with(density) {
                        height.toPx() * 0.4f
                    },
                )
            }
        }
    }
}

@Composable
private fun BetweenLoaded(
    modifier: Modifier = Modifier,
    uiState: RootHomeTabPeriodContentUiState.LoadingState.Loaded,
) {
    Column(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 4.dp),
        ) {
            var expanded by remember { mutableStateOf(false) }
            DropDownMenuButton(
                modifier = Modifier,
                onClick = { expanded = !expanded },
            ) {
                Text(uiState.categoryType)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                uiState.categoryTypes.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(type.title)
                        },
                        onClick = { type.onClick() }
                    )
                }
            }
        }
        when (uiState.graphContent) {
            is RootHomeTabPeriodContentUiState.GraphContent.Category -> {
                CategoryContent(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState.graphContent,
                )
            }

            is RootHomeTabPeriodContentUiState.GraphContent.Total -> {
                TotalContent(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState.graphContent,
                )
            }
        }

    }
}

@Composable
private fun CategoryContent(
    modifier: Modifier = Modifier,
    uiState: RootHomeTabPeriodContentUiState.GraphContent.Category,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(12.dp))
        Card {
            PolygonalLineGraph(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(200.dp),
                graphItems = uiState.graphItems,
                contentColor = LocalContentColor.current,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                uiState.monthTotalItems.forEach {
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TotalContent(
    modifier: Modifier = Modifier,
    uiState: RootHomeTabPeriodContentUiState.GraphContent.Total,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            BarGraph(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp),
                uiState = uiState.barGraph,
                contentColor = LocalContentColor.current,
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                uiState.totalBarColorTextMapping.forEach {
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
            uiState.monthTotalItems.forEach {
                Row {
                    Text(it.title)
                    Spacer(modifier = Modifier.widthIn(min = 8.dp).weight(1f))
                    Text(it.amount)
                }
            }
        }
    }
}

@Composable
private fun PeriodSection(
    modifier: Modifier = Modifier,
    onClickPreviousMonth: () -> Unit,
    onClickNextMonth: () -> Unit,
    betweenText: @Composable () -> Unit,
    rangeText: @Composable () -> Unit,
    onClickRange: (range: Int) -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.clip(CircleShape)
                    .clickable { onClickPreviousMonth() }
                    .padding(8.dp),
            ) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "前の月")
            }
            Box(
                modifier = Modifier.clip(CircleShape)
                    .clickable { onClickNextMonth() }
                    .padding(8.dp),
            ) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "次の月")
            }
        }
        betweenText()
        Spacer(Modifier.weight(1f))
        Box {
            var expanded by remember { mutableStateOf(false) }
            DropDownMenuButton(
                onClick = { expanded = !expanded },
            ) {
                rangeText()
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onClickRange(3)
                    },
                    text = {
                        Text("3ヶ月")
                    },
                )
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onClickRange(6)
                    },
                    text = {
                        Text("6ヶ月")
                    },
                )
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onClickRange(12)
                    },
                    text = {
                        Text("12ヶ月")
                    },
                )
            }
        }
    }
}
