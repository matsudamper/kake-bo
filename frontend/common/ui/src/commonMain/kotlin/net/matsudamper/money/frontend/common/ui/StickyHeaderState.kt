package net.matsudamper.money.frontend.common.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import kotlin.math.roundToInt

@Stable
public class StickyHeaderState(
    internal val enterAlways: Boolean,
) {
    internal var headerHeight by mutableStateOf(0)
    internal var scrolled by mutableStateOf(0f)
    internal var listState: ScrollableState? by mutableStateOf(null)
}

private class NestedScrollConnectionImpl(
    private val listState: ScrollableState,
    private val stickyHeaderState: StickyHeaderState,
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val listState = listState
        if (!stickyHeaderState.enterAlways) {
            if (listState.canScrollBackward && available.y > 0) {
                return Offset.Zero
            }
        }

        val newScrolled = (stickyHeaderState.scrolled + available.y)
            .coerceAtLeast(-stickyHeaderState.headerHeight.toFloat())
            .coerceAtMost(0f)
        val consume = newScrolled - stickyHeaderState.scrolled
        stickyHeaderState.scrolled = newScrolled

        return Offset(0f, consume)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        return Velocity.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        return onPreScroll(available, source)
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return Velocity.Zero
    }
}

public fun Modifier.stickyHeader(state: StickyHeaderState): Modifier {
    return this then StickyHeaderModifierNodeElement(
        scrolled = state.scrolled,
        updateHeight = { height ->
            state.headerHeight = height
        },
    )
}

public fun Modifier.stickyHeaderScrollable(
    listState: ScrollableState,
    state: StickyHeaderState,
): Modifier {
    return this.composed {
        nestedScroll(
            remember(listState, state) {
                NestedScrollConnectionImpl(
                    listState = listState,
                    stickyHeaderState = state,
                )
            },
        )
    }
}

public fun Modifier.stickyHeaderContentScrollable(
    state: StickyHeaderState,
): Modifier {
    return this.composed {
        val scrollableState = remember(state) {
            ScrollableState { delta ->
                val listState = state.listState
                // enterAlways=false かつ下スクロール時にリストが上にスクロール済みなら先にリストをスクロール
                if (!state.enterAlways && delta > 0 && listState != null && listState.canScrollBackward) {
                    val consumedByList = listState.dispatchRawDelta(delta)
                    val remaining = delta - consumedByList
                    val newScrolled = (state.scrolled + remaining)
                        .coerceAtLeast(-state.headerHeight.toFloat())
                        .coerceAtMost(0f)
                    val consumedByHeader = newScrolled - state.scrolled
                    state.scrolled = newScrolled
                    consumedByList + consumedByHeader
                } else {
                    // ヘッダーの折りたたみ/展開を先に処理し、残りをリストに転送
                    val newScrolled = (state.scrolled + delta)
                        .coerceAtLeast(-state.headerHeight.toFloat())
                        .coerceAtMost(0f)
                    val consumedByHeader = newScrolled - state.scrolled
                    state.scrolled = newScrolled
                    val remaining = delta - consumedByHeader
                    val consumedByList = if (listState != null && remaining != 0f) {
                        listState.dispatchRawDelta(remaining)
                    } else {
                        0f
                    }
                    consumedByHeader + consumedByList
                }
            }
        }
        scrollable(
            state = scrollableState,
            orientation = Orientation.Vertical,
        )
    }
}

internal class StickyHeaderModifierNodeElement(
    internal var scrolled: Float,
    internal var updateHeight: (Int) -> Unit,
) : ModifierNodeElement<StickyHeaderModifierNode>() {
    override fun create(): StickyHeaderModifierNode {
        return StickyHeaderModifierNode(
            scrolled = scrolled,
            updateHeight = updateHeight,
        )
    }

    override fun update(node: StickyHeaderModifierNode) {
        node.scrolled = scrolled
        node.updateHeight = updateHeight
    }

    override fun hashCode(): Int = scrolled.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other !is StickyHeaderModifierNodeElement) return false
        return other.scrolled == scrolled
    }
}

internal class StickyHeaderModifierNode(
    internal var scrolled: Float,
    internal var updateHeight: (Int) -> Unit,
) : LayoutModifierNode, Modifier.Node() {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        updateHeight(placeable.height)
        val containerHeight = (placeable.height + scrolled).roundToInt()
        return layout(
            width = placeable.width,
            height = containerHeight,
        ) {
            placeable.place(0, Alignment.Bottom.align(placeable.height, containerHeight))
        }
    }
}
