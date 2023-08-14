package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.ScrollButtons
import net.matsudamper.money.frontend.common.ui.ScrollButtonsDefaults
import net.matsudamper.money.frontend.common.ui.base.DropDownMenuButton
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.layout.graph.BarGraph
import net.matsudamper.money.frontend.common.ui.layout.graph.PolygonalLineGraph

@Composable
public fun RootHomeTabScreen(
    uiState: RootHomeTabUiState,
    scaffoldListener: RootScreenScaffoldListener,
) {
    LaunchedEffect(uiState.event) {
        uiState.event.onViewInitialized()
    }
    RootScreenScaffold(
        modifier = Modifier.fillMaxSize(),
        currentScreen = RootScreenTab.Home,
        listener = scaffoldListener,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            scaffoldListener.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
            )
        },
        content = {
            when (val screenState = uiState.screenState) {
                is RootHomeTabUiState.ScreenState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                is RootHomeTabUiState.ScreenState.Loaded -> {
                    MainContent(
                        uiState = screenState,
                    )
                }

                is RootHomeTabUiState.ScreenState.Error -> {
                    LoadingErrorContent(
                        modifier = Modifier.fillMaxSize(),
                        onClickRetry = {
                            // TODO
                        },
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    uiState: RootHomeTabUiState.ScreenState.Loaded,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            AssistChip(
                onClick = {},
                label = {
                    Text("月別")
                },
            )
            AssistChip(
                onClick = {},
                label = {
                    Text("期間")
                },
            )
        }
        when (uiState.displayType) {
            is RootHomeTabUiState.DisplayType.Between -> {
                BetweenContent(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState.displayType,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BetweenContent(
    modifier: Modifier = Modifier,
    uiState: RootHomeTabUiState.DisplayType.Between,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val height = this.maxHeight
        val scrollState = rememberScrollState()
        val density = LocalDensity.current
        Column(
            modifier = modifier
                .verticalScroll(scrollState),
        ) {
            PeriodSection(
                modifier = Modifier.fillMaxWidth(),
                onClickPreviousMonth = { uiState.event.onClickPreviousMonth() },
                onClickNextMonth = { uiState.event.onClickNextMonth() },
                betweenText = {
                    Text(uiState.between)
                },
                rangeText = {
                    Text(uiState.rangeText)
                },
                onClickRange = { range -> uiState.event.onClickRange(range) },
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp),
            ) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 8.dp),
                    text = "合計",
                )
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        BarGraph(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(600.dp),
                            uiState = uiState.totalBar,
                            contentColor = LocalContentColor.current,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(modifier = Modifier.fillMaxWidth()) {
                            uiState.totalBarColorTextMapping.forEach {
                                Card {
                                    Row(
                                        modifier = Modifier.padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
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
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Card {
                    PolygonalLineGraph(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .height(200.dp),
                        graphItems = uiState.totals,
                        contentColor = LocalContentColor.current,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        uiState.totals.forEach {
                            Row {
                                Text("${it.year}/${it.month.toString().padStart(2, '0')}")
                                Spacer(modifier = Modifier.widthIn(min = 8.dp).weight(1f))
                                Text("${it.amount}円")
                            }
                        }
                    }
                }
            }
        }
        ScrollButtons(
            modifier = Modifier
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
