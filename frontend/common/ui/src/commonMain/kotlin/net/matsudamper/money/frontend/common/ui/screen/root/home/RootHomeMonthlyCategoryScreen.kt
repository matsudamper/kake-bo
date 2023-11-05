package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.ScrollButtons
import net.matsudamper.money.frontend.common.ui.ScrollButtonsDefaults
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
    public data class Item(
        val title: String,
        val amount: String,
        val subCategory: String,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onClick()
        }
    }

    public sealed interface LoadingState {
        public data class Loaded(
            val items: List<Item>,
        ) : LoadingState

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
        modifier = modifier,
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
                LoadedContent(
                    modifier = Modifier.fillMaxWidth(),
                    loadingState = loadingState,
                )
            }

            RootHomeMonthlyCategoryScreenUiState.LoadingState.Loading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            RootHomeMonthlyCategoryScreenUiState.LoadingState.Error -> {
                LoadingErrorContent(
                    modifier = Modifier,
                    onClickRetry = { /* TODO */ },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedContent(
    loadingState: RootHomeMonthlyCategoryScreenUiState.LoadingState.Loaded,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val lazyListState = rememberLazyListState()
        val height by rememberUpdatedState(maxHeight)
        var scrollButtonHeight by remember { mutableIntStateOf(0) }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(
                bottom = with(density) { scrollButtonHeight.toDp() } + 8.dp,
                top = 8.dp,
                start = 8.dp,
                end = 8.dp,
            ),
        ) {
            items(loadingState.items) { item ->
                Card(
                    modifier = Modifier.padding(vertical = 2.dp),
                    onClick = {
                        item.event.onClick()
                    },
                ) {
                    ProvideTextStyle(
                        MaterialTheme.typography.bodyMedium,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = item.title,
                                maxLines = 3,
                            )
                            Text(
                                modifier = Modifier.requiredWidthIn(min = 80.dp),
                                text = item.subCategory,
                                maxLines = 1,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                modifier = Modifier.requiredWidthIn(min = 60.dp),
                                maxLines = 1,
                                text = item.amount,
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                }
            }
        }
        ScrollButtons(
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(ScrollButtonsDefaults.padding)
                .height(ScrollButtonsDefaults.height)
                .onSizeChanged {
                    scrollButtonHeight = it.height
                },
            scrollState = lazyListState,
            scrollSize = with(density) { height.toPx() } * 0.4f,
        )
    }
}
