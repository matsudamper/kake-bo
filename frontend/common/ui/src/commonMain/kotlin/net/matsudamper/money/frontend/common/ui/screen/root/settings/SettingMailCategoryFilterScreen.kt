package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.generated.resources.Res
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_add
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_arrow_back
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.FullScreenTextInput
import org.jetbrains.compose.resources.painterResource

private val FabPadding = 16.dp

public data class SettingMailCategoryFilterScreenUiState(
    public val event: Event,
    public val textInput: TextInput?,
    public val loadingState: LoadingState,
    public val kakeboScaffoldListener: KakeboScaffoldListener,
) {
    public data class TextInput(
        val title: String,
        val onCompleted: (String) -> Unit,
        val dismiss: () -> Unit,
    )

    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data object Error : LoadingState

        public data class Loaded(
            val filters: ImmutableList<Item>,
            val isError: Boolean,
            val loadToEnd: Boolean,
            val isRefreshing: Boolean,
            val event: LoadedEvent,
        ) : LoadingState
    }

    public data class Item(
        val title: String,
        val event: ItemEvent,
    )

    @Immutable
    public interface LoadedEvent {
        public fun onClickAdd()

        public fun onPullToRefresh()

        public fun loadMore()
    }

    @Immutable
    public interface ItemEvent {
        public fun onClick()
    }

    @Immutable
    public interface Event {
        public fun onClickRetry()

        public fun onViewInitialized()

        public fun onClickBack()
    }
}

@Composable
public fun SettingMailCategoryFiltersScreen(
    modifier: Modifier = Modifier,
    uiState: SettingMailCategoryFilterScreenUiState,
    windowInsets: PaddingValues,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    uiState.textInput?.also { textInput ->
        FullScreenTextInput(
            title = textInput.title,
            onComplete = { textInput.onCompleted(it) },
            canceled = { textInput.dismiss() },
            default = "",
            isMultiline = false,
        )
    }
    RootScreenScaffold(
        modifier = modifier,
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                navigation = {
                    IconButton(onClick = { uiState.event.onClickBack() }) {
                        Icon(painter = painterResource(Res.drawable.ic_arrow_back), contentDescription = null)
                    }
                },
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            uiState.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
                windowInsets = windowInsets,
            )
        },
    ) {
        SettingScaffold(
            title = {
                Text("メールカテゴリフィルタ一覧")
            },
            fab = {
                when (val loadingState = uiState.loadingState) {
                    is SettingMailCategoryFilterScreenUiState.LoadingState.Error,
                    is SettingMailCategoryFilterScreenUiState.LoadingState.Loading,
                    -> Unit

                    is SettingMailCategoryFilterScreenUiState.LoadingState.Loaded -> {
                        FloatingActionButton(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(
                                    end = FabPadding,
                                    bottom = FabPadding,
                                ),
                            onClick = { loadingState.event.onClickAdd() },
                        ) {
                            Icon(painter = painterResource(Res.drawable.ic_add), contentDescription = "フィルタを追加")
                        }
                    }
                }
            },
        ) { paddingValues ->
            when (val loadingState = uiState.loadingState) {
                is SettingMailCategoryFilterScreenUiState.LoadingState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .padding(paddingValues),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                is SettingMailCategoryFilterScreenUiState.LoadingState.Loaded -> {
                    LoadedContent(
                        modifier = Modifier.fillMaxSize(),
                        uiState = loadingState,
                        contentPadding = paddingValues,
                    )
                }

                SettingMailCategoryFilterScreenUiState.LoadingState.Error -> {
                    LoadingErrorContent(
                        modifier = Modifier.fillMaxSize()
                            .padding(paddingValues),
                        onClickRetry = uiState.event::onClickRetry,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedContent(
    modifier: Modifier = Modifier,
    uiState: SettingMailCategoryFilterScreenUiState.LoadingState.Loaded,
    contentPadding: PaddingValues,
) {
    val layoutDirection = LocalLayoutDirection.current
    val lazyListState = rememberLazyListState()
    val fabSize = 56.dp
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        modifier = modifier,
        state = pullToRefreshState,
        onRefresh = { uiState.event.onPullToRefresh() },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                start = contentPadding.calculateStartPadding(layoutDirection),
                end = contentPadding.calculateEndPadding(layoutDirection),
                bottom = contentPadding.calculateBottomPadding() + fabSize + (FabPadding * 2),
            ),
        ) {
            items(uiState.filters) { item ->
                SettingListMenuItemButton(onClick = { item.event.onClick() }) {
                    Text(item.title)
                }
            }
            if (uiState.loadToEnd.not()) {
                item {
                    LaunchedEffect(Unit) {
                        uiState.event.loadMore()
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
        }
    }
}
