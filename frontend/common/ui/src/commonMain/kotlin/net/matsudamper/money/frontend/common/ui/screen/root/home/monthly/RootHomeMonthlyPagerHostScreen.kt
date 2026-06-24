package net.matsudamper.money.frontend.common.ui.screen.root.home.monthly

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabScreenScaffold

@Stable
public data class RootHomeMonthlyPagerHostScreenUiState(
    val kakeboScaffoldListener: KakeboScaffoldListener,
    val pages: ImmutableList<Page>,
    val currentPage: Int,
    val showImages: Boolean,
    val event: Event,
) {
    public data class Page(
        val navigation: RootHomeScreenStructure.Monthly,
    )

    @Immutable
    public interface Event {
        public fun onPageChanged(page: Page)
        public fun onToggleShowImages()
    }
}

@Composable
public fun RootHomeMonthlyPagerHostScreen(
    uiState: RootHomeMonthlyPagerHostScreenUiState,
    uiStateProvider: @Composable (RootHomeScreenStructure.Monthly) -> RootHomeMonthlyScreenUiState,
    windowInsets: PaddingValues,
    modifier: Modifier = Modifier,
) {
    RootHomeTabScreenScaffold(
        kakeboScaffoldListener = uiState.kakeboScaffoldListener,
        modifier = modifier,
        windowInsets = windowInsets,
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.padding(end = 4.dp),
                    style = MaterialTheme.typography.titleMedium,
                    text = "画像",
                )
                Switch(
                    checked = uiState.showImages,
                    onCheckedChange = { uiState.event.onToggleShowImages() },
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        },
        content = {
            val state = rememberPagerState(uiState.currentPage) { uiState.pages.size }
            LaunchedEffect(state, uiState.currentPage) {
                if (state.currentPage != uiState.currentPage) {
                    state.animateScrollToPage(
                        uiState.currentPage,
                        animationSpec = tween(durationMillis = 300),
                    )
                }
            }
            var beforePage: Int? by rememberSaveable { mutableStateOf(null) }
            val event by rememberUpdatedState(uiState.event)
            LaunchedEffect(state) {
                snapshotFlow { state.settledPage }.collect { settledPage ->
                    if (beforePage == null) {
                        beforePage = settledPage
                        return@collect
                    }
                    if (beforePage == settledPage) {
                        return@collect
                    }
                    beforePage = settledPage

                    val page = uiState.pages.getOrNull(settledPage) ?: return@collect
                    event.onPageChanged(page)
                }
            }
            HorizontalPager(
                state = state,
            ) { index ->
                val item = uiState.pages[index]

                RootHomeMonthlyScreen(
                    modifier = Modifier,
                    uiState = uiStateProvider(item.navigation),
                    showImages = uiState.showImages,
                    windowInsets = windowInsets,
                )
            }
        },
    )
}
