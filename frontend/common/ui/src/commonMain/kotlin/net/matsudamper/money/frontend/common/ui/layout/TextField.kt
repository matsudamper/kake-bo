package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

public object TextFieldDefaults {
    @Composable
    public fun colors(): TextFieldColors {
        return TextFieldDefaults.colors(
            focusedSupportingTextColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            focusedPrefixColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedSuffixColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

public enum class TextFieldType {
    Text,
    Password,
}

@Composable
public fun TextField(
    text: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    type: TextFieldType = TextFieldType.Text,
    textStyle: TextStyle = LocalTextStyle.current,
    label: String? = null,
    maxLines: Int = Int.MAX_VALUE,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    isError: Boolean = false,
    shape: Shape = TextFieldDefaults.shape,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: TextFieldColors = TextFieldDefaults.colors(),
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
        visualTransformation = when (type) {
            TextFieldType.Text -> VisualTransformation.None
            TextFieldType.Password -> PasswordVisualTransformation()
        },
    )
}
