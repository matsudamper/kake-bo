package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable

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
