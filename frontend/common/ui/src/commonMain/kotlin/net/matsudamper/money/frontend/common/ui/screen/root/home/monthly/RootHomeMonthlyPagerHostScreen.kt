package net.matsudamper.money.frontend.common.ui.screen.root.home.monthly

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabScreenScaffold

@Stable
public data class RootHomeMonthlyPagerHostScreenUiState(
    val scaffoldListener: RootScreenScaffoldListener,
    val pages: ImmutableList<Page>,
    val currentPage: Int,
) {
    public data class Page(
        val navigation: RootHomeScreenStructure.Monthly,
    )
}

@Composable
public fun RootHomeMonthlyPagerHostScreen(
    uiState: RootHomeMonthlyPagerHostScreenUiState,
    uiStateProvider: @Composable (RootHomeScreenStructure.Monthly) -> RootHomeMonthlyScreenUiState,
    windowInsets: PaddingValues,
    modifier: Modifier = Modifier,
) {
    RootHomeTabScreenScaffold(
        scaffoldListener = uiState.scaffoldListener,
        modifier = modifier,
        windowInsets = windowInsets,
        content = {
            val state = rememberPagerState(uiState.currentPage) { uiState.pages.size }
            LaunchedEffect(state, uiState.currentPage) {
                state.animateScrollToPage(
                    uiState.currentPage,
                    animationSpec = tween(durationMillis = 300),
                )
            }
            HorizontalPager(
                state = state,
            ) { index ->
                val item = uiState.pages[index]

                RootHomeMonthlyScreen(
                    modifier = Modifier,
                    uiState = uiStateProvider(item.navigation),
                    windowInsets = windowInsets,
                )
            }
        },
    )
}
