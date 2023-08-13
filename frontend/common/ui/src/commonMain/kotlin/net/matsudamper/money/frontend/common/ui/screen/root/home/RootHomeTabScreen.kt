package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.clip(CircleShape)
                            .clickable { uiState.event.onClickPreviousMonth() }
                            .padding(8.dp),
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "前の月")
                    }
                    Box(
                        modifier = Modifier.clip(CircleShape)
                            .clickable { uiState.event.onClickNextMonth() }
                            .padding(8.dp),
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "次の月")
                    }
                }
                Text(uiState.between)
                Spacer(Modifier.weight(1f))
                DropDownMenuButton(
                    onClick = {},
                ) {
                    Text("3ヶ月")
                }
            }
            uiState.totals.forEach {
                Text("${it.year}年${it.month}月: ${it.amount}")
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
