package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun <T> DrumPicker(
    items: List<T>,
    initialIndex: Int,
    itemHeight: Dp,
    rows: Int,
    modifier: Modifier = Modifier,
    onSelectedIndexChanged: (Int) -> Unit,
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit,
) {
    require(rows % 2 != 0) { "rows must be odd" }
    val density = LocalDensity.current
    val lazyListState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)

    val centerItemIndex by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty() || layoutInfo.totalItemsCount == 0) {
                -1
            } else {
                val viewportCenterY = lazyListState.layoutInfo.viewportSize.height / 2f
                val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                val closestItem = visibleItems.minByOrNull {
                    abs(it.offset + it.size / 2 - viewportCenterY)
                }
                closestItem?.index ?: -1
            }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { centerItemIndex }
            .distinctUntilChanged()
            .filter { it != -1 }
            .collect { index ->
                if (index in items.indices) {
                    onSelectedIndexChanged(index)
                }
            }
    }

    var isInitialScrolled by remember { mutableStateOf(false) }
    LaunchedEffect(lazyListState.layoutInfo.totalItemsCount) {
        if (isInitialScrolled.not() && lazyListState.layoutInfo.totalItemsCount > 0) {
            val targetIndex = initialIndex
            with(density) {
                lazyListState.scrollToItem(
                    index = targetIndex,
                    scrollOffset = (-lazyListState.layoutInfo.viewportSize.height / 2 + itemHeight.roundToPx() / 2),
                )
            }
            isInitialScrolled = true
        }
    }


    Box(
        modifier = modifier
            .height(itemHeight * rows),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.matchParentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            flingBehavior = flingBehavior,
        ) {
            val topAndBottomEmptyRows = rows / 2
            items(topAndBottomEmptyRows) {
                Box(modifier = Modifier.height(itemHeight))
            }
            items(
                count = items.size,
                key = { index -> items[index].hashCode() },
            ) { index ->
                val adjustedIndex = index + topAndBottomEmptyRows
                val isSelected = centerItemIndex == adjustedIndex
                Box(
                    modifier = Modifier.height(itemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    itemContent(items[index], isSelected)
                }
            }
            items(topAndBottomEmptyRows) {
                Box(modifier = Modifier.height(itemHeight))
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = -itemHeight / 2),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        )
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = itemHeight / 2),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        )
    }
}
