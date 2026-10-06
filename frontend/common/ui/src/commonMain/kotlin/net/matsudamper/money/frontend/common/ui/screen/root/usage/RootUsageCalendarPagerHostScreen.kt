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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.drop
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.StickyHeaderState

@Stable
public data class RootUsageCalendarPagerHostScreenUiState(
    val pages: ImmutableList<Page>,
    val hostScreenUiState: RootUsageHostScreenUiState,
    val currentPage: Int,
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
    val state = rememberPagerState(uiState.currentPage) { uiState.pages.size }
    LaunchedEffect(state, uiState.currentPage) {
        if (state.currentPage != uiState.currentPage) {
            state.animateScrollToPage(
                uiState.currentPage,
                animationSpec = tween(durationMillis = 300),
            )
        }
    }
    val latestUiState by rememberUpdatedState(uiState)
    LaunchedEffect(state) {
        snapshotFlow { state.settledPage }
            .drop(1)
            .collect { settledPage ->
                val page = latestUiState.pages.getOrNull(settledPage) ?: return@collect
                latestUiState.event.onPageChanged(page)
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
