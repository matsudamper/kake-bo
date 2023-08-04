package net.matsudamper.money.frontend.common.ui.screen.root

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.layout.ScrollButton
import net.matsudamper.money.frontend.common.ui.layout.ScrollButtonDefaults

public data class RootListScreenUiState(
    val event: Event,
    val loadingState: LoadingState,
) {
    @Immutable
    public sealed interface LoadingState {
        public object Loading : LoadingState
        public data class Loaded(
            val loadToEnd: Boolean,
            val items: List<Item>,
            val event: LoadedEvent,
        ) : LoadingState
    }

    public data class Item(
        val title: String,
        val date: String,
        val amount: String,
        val category: String?,
        val description: String,
    )

    @Immutable
    public interface LoadedEvent {
        public fun loadMore()
    }

    @Immutable
    public interface Event {
        public fun onViewInitialized()
        public fun onClickAdd()
    }
}

@Composable
public fun RootListScreen(
    modifier: Modifier = Modifier,
    uiState: RootListScreenUiState,
    listener: RootScreenScaffoldListener,
) {
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }

    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        currentScreen = RootScreenTab.List,
        listener = listener,
        content = {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val height = maxHeight
                when (uiState.loadingState) {
                    is RootListScreenUiState.LoadingState.Loaded -> {
                        val lazyListState = rememberLazyListState()
                        LoadedContent(
                            modifier = Modifier.fillMaxSize(),
                            uiState = uiState.loadingState,
                            paddingValues = PaddingValues(
                                end = ScrollButtonDefaults.scrollButtonHorizontalPadding * 2 + ScrollButtonDefaults.scrollButtonSize,
                            ),
                            lazyListState = lazyListState,
                        )

                        ScrollButton(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(ScrollButtonDefaults.scrollButtonHorizontalPadding)
                                .width(ScrollButtonDefaults.scrollButtonSize),
                            scrollState = lazyListState,
                            scrollSize = with(density) {
                                height.toPx() * 0.7f
                            },
                            animationSpec = spring(
                                stiffness = Spring.StiffnessLow,
                            ),
                        )
                    }

                    is RootListScreenUiState.LoadingState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                FloatingActionButton(
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .padding(end = ScrollButtonDefaults.scrollButtonHorizontalPadding * 2 + ScrollButtonDefaults.scrollButtonSize)
                        .padding(bottom = 24.dp, end = 12.dp),
                    onClick = { uiState.event.onClickAdd() },
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "add money usage",
                    )
                }
            }
        },
    )
}

@Composable
private fun LoadedContent(
    modifier: Modifier = Modifier,
    uiState: RootListScreenUiState.LoadingState.Loaded,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
) {
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
            end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
        ),
    ) {
        item {
            Spacer(modifier = Modifier.heightIn(paddingValues.calculateTopPadding()))
        }
        items(uiState.items) { item ->
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 6.dp,
                    )
                    .padding(start = 12.dp),
                item = item,
            )
        }
        if (uiState.loadToEnd.not()) {
            item {
                LaunchedEffect(Unit) {
                    uiState.event.loadMore()
                    while (isActive) {
                        delay(500)
                        uiState.event.loadMore()
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
        item {
            Spacer(modifier = Modifier.heightIn(paddingValues.calculateBottomPadding()))
        }
    }
}

@Composable
private fun ListItem(
    modifier: Modifier = Modifier,
    item: RootListScreenUiState.Item,
) {
    Card(
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = item.title)
            Spacer(modifier = Modifier.heightIn(4.dp))
            Text(text = item.description)
            Spacer(modifier = Modifier.heightIn(4.dp))
            Text(text = item.date)
            Spacer(modifier = Modifier.heightIn(4.dp))
            Text(text = item.amount)
            Spacer(modifier = Modifier.heightIn(4.dp))
            Text(text = item.category ?: "")
            Spacer(modifier = Modifier.heightIn(4.dp))
            Text(text = item.description)
        }
    }
}
