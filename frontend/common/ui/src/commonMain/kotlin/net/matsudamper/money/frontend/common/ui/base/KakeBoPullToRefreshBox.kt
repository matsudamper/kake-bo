package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput

/**
 * マウスホイールのスクロールも nested scroll として届き、PC で一覧の先頭からホイールを回すだけで
 * 更新が始まってしまうため、マウス操作中は引っ張って更新に余ったスクロールを渡さない
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun KakeBoPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier,
    state: PullToRefreshState,
    content: @Composable BoxScope.() -> Unit,
) {
    val mouseScrollBlocker = remember { MouseScrollBlockingConnection() }
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pointerType = event.changes.firstOrNull()?.type ?: continue
                        mouseScrollBlocker.isMouseOperating = pointerType == PointerType.Mouse
                    }
                }
            }
            .pullToRefresh(
                isRefreshing = isRefreshing,
                state = state,
                onRefresh = onRefresh,
            )
            .nestedScroll(mouseScrollBlocker),
    ) {
        content()
        PullToRefreshDefaults.Indicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isRefreshing = isRefreshing,
            state = state,
        )
    }
}

/**
 * 再コンポーズを待たずにホイールの最初の差分から止めるため、State ではなく即時に読めるフィールドで持つ
 */
private class MouseScrollBlockingConnection : NestedScrollConnection {
    var isMouseOperating: Boolean = false

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        return if (isMouseOperating) available else Offset.Zero
    }
}
