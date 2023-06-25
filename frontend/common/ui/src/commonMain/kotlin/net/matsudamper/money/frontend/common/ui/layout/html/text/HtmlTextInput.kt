package net.matsudamper.money.frontend.common.ui.layout.html.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
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

@Composable
public fun HtmlText(
    modifier: Modifier = Modifier,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val htmlTextContext = LocalHtmlTextContext.current
    val id = remember { Random.nextULong().toString() }
    var size by remember { mutableStateOf(Size.Zero) }
    val localDensity = LocalDensity.current

    LaunchedEffect(text) {
        htmlTextContext.setText(id, text)
    }
    DisposableEffect(Unit) {
        onDispose {
            htmlTextContext.remove(id)
        }
    }
    LaunchedEffect(textColor) {
        println("send color: ${textColor}")
        htmlTextContext.setTextColor(id, textColor)
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
                    maxWidth = constraints.maxWidth,
                    maxHeight = constraints.maxHeight,
                )

                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }

            }
            .onGloballyPositioned { newPosition ->
                htmlTextContext.setPosition(
                    id,
                    newPosition.positionInWindow(),
                )
            },
    )
}
