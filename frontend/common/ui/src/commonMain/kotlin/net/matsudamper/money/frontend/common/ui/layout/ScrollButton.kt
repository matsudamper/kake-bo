package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
public fun ScrollButton(
    modifier: Modifier = Modifier,
    scrollState: ScrollableState,
    scrollSize: Float,
    animationSpec: AnimationSpec<Float>,
) {
    val coroutineScope = rememberCoroutineScope()
    Box(modifier = modifier) {
        val size = 38.dp
        Button(
            modifier = Modifier.align(Alignment.TopEnd),
            shape = CircleShape,
            contentPadding = PaddingValues(12.dp),
            onClick = {
                coroutineScope.launch {
                    scrollState.animateScrollBy(
                        value = -scrollSize,
                        animationSpec = animationSpec,
                    )
                }
            },
        ) {
            Icon(
                modifier = Modifier
                    .size(size),
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
            )
        }
        Button(
            modifier = Modifier.align(Alignment.BottomEnd),
            shape = CircleShape,
            contentPadding = PaddingValues(12.dp),
            onClick = {
                coroutineScope.launch {
                    scrollState.animateScrollBy(
                        value = scrollSize,
                        animationSpec = animationSpec,
                    )
                }
            },
        ) {
            Icon(
                modifier = Modifier
                    .size(size),
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
            )
        }
    }
}
