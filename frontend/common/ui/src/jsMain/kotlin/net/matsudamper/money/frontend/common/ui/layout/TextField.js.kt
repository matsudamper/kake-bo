package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public actual fun TextField(
    text: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    type: TextFieldType,
    textStyle: TextStyle,
    label: String?,
    maxLines: Int,
    placeholder: String?,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    prefix: @Composable (() -> Unit)?,
    suffix: @Composable (() -> Unit)?,
    supportingText: @Composable (() -> Unit)?,
    enabled: Boolean,
    singleLine: Boolean,
    isError: Boolean,
    shape: Shape,
    interactionSource: MutableInteractionSource,
    colors: TextFieldColors,
) {
    var visibleInput by remember { mutableStateOf(false) }
    if (visibleInput) {
        HtmlFullScreenTextInput(
            title = label ?: "",
            name = label ?: "",
            default = text,
            inputType = when (type) {
                TextFieldType.Text -> "text"
                TextFieldType.Password -> "password"
            },
            onComplete = {
                onValueChange(it)
                visibleInput = false
            },
            isMultiline = false,
            canceled = { visibleInput = false },
        )
    }
    Box(
        modifier = modifier.clickable {
            visibleInput = false
        },
    ) {
        TextFieldDefaults.DecorationBox(
            value = text,
            visualTransformation = VisualTransformation.None,
            innerTextField = {
                Text(
                    modifier = Modifier,
                    text = text,
                    style = textStyle,
                    maxLines = maxLines,
                )
            },
            placeholder = run {
                placeholder ?: return@run null
                //
                {
                    Text(placeholder)
                }
            },
            label = run {
                label ?: return@run null
                //
                {
                    Text(label)
                }
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            shape = shape,
            singleLine = singleLine,
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            colors = colors,
        )
    }
}
