package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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

internal object ElongatedScrollButtonDefaults {
    internal val scrollButtonSize = 52.dp
    internal val scrollButtonHorizontalPadding = 8.dp
}

@Composable
public fun ElongatedScrollButton(
    modifier: Modifier = Modifier,
    scrollState: ScrollableState,
    scrollSize: Float,
    animationSpec: AnimationSpec<Float> = spring(
        stiffness = Spring.StiffnessLow,
    ),
) {
    val coroutineScope = rememberCoroutineScope()
    Box(modifier = modifier.fillMaxHeight()) {
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
                modifier = Modifier.fillMaxWidth()
                    .aspectRatio(1f),
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
            )
        }
        Button(
            modifier = Modifier.align(Alignment.BottomEnd)
                .fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth()
                    .aspectRatio(1f),
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
            )
        }
    }
}
