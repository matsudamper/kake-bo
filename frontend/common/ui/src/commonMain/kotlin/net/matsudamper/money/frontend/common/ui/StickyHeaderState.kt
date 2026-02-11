package net.matsudamper.money.frontend.common.ui

import androidx.compose.foundation.gestures.ScrollableState
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
