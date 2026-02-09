package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType

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
    autocomplete: String?,
) {
    androidx.compose.material3.TextField(
        value = text,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        label = run {
            label ?: return@run null
            //
            {
                Text(label)
            }
        },
        maxLines = maxLines,
        placeholder = run {
            placeholder ?: return@run null
            //
            {
                Text(placeholder)
            }
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        enabled = enabled,
        singleLine = singleLine,
        isError = isError,
        shape = shape,
        interactionSource = interactionSource,
        colors = colors,
        keyboardOptions = when (type) {
            TextFieldType.Text -> {
                KeyboardOptions.Default
            }

            TextFieldType.Password -> {
                KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                )
            }
        },
    )
}
