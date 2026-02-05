package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier

private const val INITIAL_PAGE = 500
private const val PAGE_COUNT = 1000

@Composable
public fun MonthlyPager(
    onNavigateToNextMonth: () -> Unit,
    onNavigateToPreviousMonth: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = INITIAL_PAGE,
        pageCount = { PAGE_COUNT },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { page ->
                if (page > INITIAL_PAGE) {
                    onNavigateToNextMonth()
                } else if (page < INITIAL_PAGE) {
                    onNavigateToPreviousMonth()
                }
            }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
    ) {
        content()
    }
}
