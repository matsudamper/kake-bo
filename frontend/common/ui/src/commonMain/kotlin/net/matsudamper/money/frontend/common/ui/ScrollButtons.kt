package net.matsudamper.money.frontend.common.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

internal object ScrollButtonsDefaults {
    val height = 58.dp
    val padding = PaddingValues(
        end = 12.dp,
        bottom = 12.dp
    )
}

@Composable
internal fun ScrollButtons(
    modifier: Modifier = Modifier,
    scrollState: ScrollableState,
    scrollSize: Float,
    animationSpec: AnimationSpec<Float> = spring(
        stiffness = Spring.StiffnessLow,
    ),
) {
    val coroutineScope = rememberCoroutineScope()
    Row(modifier = modifier) {
        Button(
            modifier = Modifier.fillMaxHeight()
                .aspectRatio(1f),
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
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            modifier = Modifier.fillMaxHeight()
                .aspectRatio(1f),
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