package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.DropDownMenuButton
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.LocalScrollToTopHandler

public data class RootHomeTabPeriodAndCategoryUiState(
    val loadingState: LoadingState,
    val event: Event,
) {
    @Immutable
    public sealed interface LoadingState {
        public data class Loaded(
            val rangeText: String,
            val between: String,
            val categoryType: String,
            val categoryTypes: ImmutableList<CategoryTypes>,
        ) : LoadingState

        public data object Loading : LoadingState

        public data object Error : LoadingState
    }

    public data class CategoryTypes(
        val title: String,
        val onClick: () -> Unit,
    )

    public data class MonthTotalItem(
        val title: String,
        val amount: String,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onClick()
        }
    }

    public data class MonthSubCategoryItem(
        val title: String,
        val amount: Long,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun RootHomeTabPeriodScaffold(
    uiState: RootHomeTabPeriodAndCategoryUiState,
    kakeboScaffoldListener: KakeboScaffoldListener,
    modifier: Modifier = Modifier,
    menu: @Composable () -> Unit = {},
    windowInsets: PaddingValues,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(uiState.event) {
        uiState.event.onViewInitialized()
    }

    RootHomeTabScreenScaffold(
        modifier = modifier,
        kakeboScaffoldListener = kakeboScaffoldListener,
        menu = menu,
        windowInsets = windowInsets,
    ) {
        var isRefreshing by rememberSaveable { mutableStateOf(false) }
        val refreshState = rememberPullToRefreshState()
        val coroutineScope = rememberCoroutineScope()
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            state = refreshState,
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                onRefresh()
                coroutineScope.launch {
                    delay(100)
                    isRefreshing = false
                }
            },
        ) {
            when (uiState.loadingState) {
                RootHomeTabPeriodAndCategoryUiState.LoadingState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                RootHomeTabPeriodAndCategoryUiState.LoadingState.Error -> {
                    LoadingErrorContent(
                        modifier = Modifier.fillMaxWidth(),
                        onClickRetry = {
                            uiState.event.onClickRetry()
                        },
                    )
                }

                is RootHomeTabPeriodAndCategoryUiState.LoadingState.Loaded -> {
                    val scrollState = rememberScrollState()
                    val scrollToTopHandler = LocalScrollToTopHandler.current
                    DisposableEffect(scrollToTopHandler, scrollState) {
                        val handler = {
                            if (scrollState.value > 0) {
                                coroutineScope.launch { scrollState.animateScrollTo(0) }
                                true
                            } else {
                                false
                            }
                        }
                        scrollToTopHandler.register(handler)
                        onDispose { scrollToTopHandler.unregister() }
                    }
                    Column(
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .padding(horizontal = 24.dp),
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        RootHomePeriodSection(
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
                            content = content,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BetweenLoaded(
    modifier: Modifier = Modifier,
    uiState: RootHomeTabPeriodAndCategoryUiState.LoadingState.Loaded,
    content: @Composable () -> Unit,
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
                modifier = Modifier.widthIn(min = 100.dp),
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
                        onClick = {
                            expanded = false
                            type.onClick()
                        },
                    )
                }
            }
        }
        content()
    }
}
