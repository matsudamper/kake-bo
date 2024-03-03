package net.matsudamper.money.frontend.common.ui.screen.root.home.monthly

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.width
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
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabScreenScaffold
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabScreenScaffoldUiState

public data class RootHomeMonthlyScreenUiState(
    val loadingState: LoadingState,
    val rootHomeTabUiState: RootHomeTabScreenScaffoldUiState,
    val event: Event,
) {
    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data object Error : LoadingState

        public data class Loaded(
            val totalAmount: String,
            val items: List<Item>,
            val hasMoreItem: Boolean,
            val event: LoadedEvent,
        ) : LoadingState
    }

    public data class Item(
        val title: String,
        val amount: String,
        val date: String,
        val category: String,
        val event: ItemEvent,
    )

    @Immutable
    public interface ItemEvent {
        public fun onClick()
    }

    @Immutable
    public interface LoadedEvent {
        public fun loadMore()
    }

    @Immutable
    public interface Event {
        public suspend fun onViewInitialized()
    }
}

@Composable
public fun RootHomeMonthlyScreen(
    uiState: RootHomeMonthlyScreenUiState,
    scaffoldListener: RootScreenScaffoldListener,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    RootHomeTabScreenScaffold(
        modifier = modifier,
        uiState = uiState.rootHomeTabUiState,
        scaffoldListener = scaffoldListener,
    ) {
        when (val loadingState = uiState.loadingState) {
            is RootHomeMonthlyScreenUiState.LoadingState.Loaded -> {
                LoadedContent(
                    modifier = Modifier.fillMaxSize(),
                    loadingState = loadingState,
                )
            }

            RootHomeMonthlyScreenUiState.LoadingState.Loading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            RootHomeMonthlyScreenUiState.LoadingState.Error -> {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedContent(
    loadingState: RootHomeMonthlyScreenUiState.LoadingState.Loaded,
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
            contentPadding =
                PaddingValues(
                    bottom = with(density) { scrollButtonHeight.toDp() } + 8.dp,
                    top = 8.dp,
                    start = 8.dp,
                    end = 8.dp,
                ),
        ) {
            item {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = loadingState.totalAmount,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            item {
                Text(
                    text = "TODO ここに円グラフ",
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(16.dp),
                )
            }
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
                        Column(
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Text(
                                text = item.date,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = item.title,
                                    maxLines = 3,
                                )
                                Text(
                                    modifier =
                                        Modifier
                                            .align(Alignment.Bottom)
                                            .height(IntrinsicSize.Max)
                                            .requiredWidthIn(min = 80.dp),
                                    text = item.category,
                                    maxLines = 1,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    modifier =
                                        Modifier
                                            .align(Alignment.Bottom)
                                            .height(IntrinsicSize.Max)
                                            .requiredWidthIn(min = 60.dp),
                                    maxLines = 1,
                                    text = item.amount,
                                    textAlign = TextAlign.End,
                                )
                            }
                        }
                    }
                }
            }
            if (loadingState.hasMoreItem) {
                item {
                    LaunchedEffect(Unit) {
                        loadingState.event.loadMore()
                    }
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(vertical = 16.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
        ScrollButtons(
            modifier =
                Modifier.align(Alignment.BottomEnd)
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
