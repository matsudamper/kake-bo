package net.matsudamper.money.frontend.common.ui.screen.root.usage

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.StickyHeaderState

@Stable
public data class RootUsageCalendarPagerHostScreenUiState(
    val pages: ImmutableList<Page>,
    val hostScreenUiState: RootUsageHostScreenUiState,
    val currentPage: Int?,
    val event: Event,
) {
    public data class Page(
        val navigation: ScreenStructure.Root.Usage.Calendar,
    )

    @Immutable
    public interface Event {
        public fun onPageChanged(page: Page)
    }
}

@Composable
public fun RootUsageCalendarPagerHostScreen(
    uiState: RootUsageCalendarPagerHostScreenUiState,
    uiStateProvider: @Composable (ScreenStructure.Root.Usage.Calendar) -> RootUsageCalendarScreenUiState,
    modifier: Modifier = Modifier,
    stickyHeaderState: StickyHeaderState,
) {
    val currentPage = uiState.currentPage
    if (currentPage != null) {
        val state = rememberPagerState(uiState.currentPage) { uiState.pages.size }
        LaunchedEffect(state, uiState.currentPage) {
            state.animateScrollToPage(
                uiState.currentPage,
                animationSpec = tween(durationMillis = 300),
            )
        }
        var beforePage: Int? by rememberSaveable { mutableStateOf(null) }
        val event by rememberUpdatedState(uiState.event)
        LaunchedEffect(state) {
            snapshotFlow { state.settledPage }.collect { settledPage ->
                if (beforePage == null) {
                    beforePage = settledPage
                    return@collect
                }
                if (beforePage == beforePage) {
                    return@collect
                }
                beforePage = settledPage

                val page = uiState.pages.getOrNull(settledPage) ?: return@collect
                event.onPageChanged(page)
            }
        }
        HorizontalPager(
            state = state,
            modifier = modifier,
        ) { index ->
            val item = uiState.pages[index]
            RootUsageCalendarScreen(
                modifier = Modifier.fillMaxSize(),
                uiState = uiStateProvider(item.navigation),
                stickyHeaderState = stickyHeaderState,
            )
        }
    }
}
