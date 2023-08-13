package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.layout.ElongatedScrollButton

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
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    uiState: RootHomeTabUiState.ScreenState.Loaded,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val height = this.maxHeight
        val lazyListState = rememberLazyListState()
        val density = LocalDensity.current
        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                state = lazyListState,
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .padding(start = 24.dp)
                            .padding(vertical = 12.dp),
                        onClick = { uiState.event.onClickMailImportButton() },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(24.dp),
                        ) {
                            Text("未インポートのメール")
                            Spacer(modifier = Modifier.weight(1f))
                            Text(uiState.notImportMailCount.toString())
                        }
                    }
                    Card(
                        modifier = Modifier
                            .padding(start = 24.dp)
                            .padding(vertical = 12.dp),
                        onClick = { uiState.event.onClickNotLinkedMailButton() },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(24.dp),
                        ) {
                            Text("未登録のメール")
                            Spacer(modifier = Modifier.weight(1f))
                            Text(uiState.importedAndNotLinkedMailCount.toString())
                        }
                    }
                }
            }

            ElongatedScrollButton(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(12.dp)
                    .width(42.dp),
                scrollState = lazyListState,
                scrollSize = with(density) {
                    height.toPx() * 0.7f
                },
                animationSpec = spring(
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
    }
}
