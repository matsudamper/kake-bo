package net.matsudamper.money.frontend.common.ui.layout.html.text.input

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.random.Random
import kotlin.random.nextULong


@Composable
public fun HtmlTextInput(
    modifier: Modifier = Modifier,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    type: KeyboardType,
) {
    val htmlTextContext = LocalHtmlTextInputContext.current
    val id = remember { Random.nextULong().toString() }
    var size by remember { mutableStateOf(Size.Zero) }
    val localDensity = LocalDensity.current

    DisposableEffect(Unit) {
        onDispose {
            htmlTextContext.remove(id)
        }
    }
    LaunchedEffect(placeholder) {
        htmlTextContext.setPlaceHolder(id, placeholder)
    }
    LaunchedEffect(type) {
        htmlTextContext.setType(id, type)
    }
    LaunchedEffect(htmlTextContext) {
        htmlTextContext.setTextCallback(id) { newText ->
            onValueChange(TextFieldValue(newText))
        }
    }
    LaunchedEffect(htmlTextContext) {
        htmlTextContext.setSizeCallback(id) { newSize ->
            size = newSize
        }
    }

    Box(
        modifier = modifier
            .size(with(localDensity) { size.toDpSize() })
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                htmlTextContext.setSize(
                    id,
                    width = placeable.width,
                    maxHeight = constraints.maxHeight,
                )

                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
            .onGloballyPositioned { newPosition ->
                htmlTextContext.setPosition(
                    id,
                    newPosition.positionInRoot(),
                )
            },
    )
}
