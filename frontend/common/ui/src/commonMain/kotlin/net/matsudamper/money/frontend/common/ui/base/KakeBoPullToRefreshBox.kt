package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput

/**
 * マウスホイールのスクロールも nested scroll として届き、PC で一覧の先頭からホイールを回すだけで
 * 更新が始まってしまうため、マウス操作中は引っ張って更新を無効にする
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun KakeBoPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState,
    content: @Composable BoxScope.() -> Unit,
) {
    var isMouseOperating by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pointerType = event.changes.firstOrNull()?.type ?: continue
                        isMouseOperating = pointerType == PointerType.Mouse
                    }
                }
            }
            .pullToRefresh(
                isRefreshing = isRefreshing,
                state = state,
                enabled = !isMouseOperating,
                onRefresh = onRefresh,
            ),
    ) {
        content()
        PullToRefreshDefaults.Indicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isRefreshing = isRefreshing,
            state = state,
        )
    }
}
