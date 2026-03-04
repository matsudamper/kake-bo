package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged

@Composable
public fun UrlBar(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFocused = LocalUrlBarFocused.current
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = modifier.onFocusChanged { focusState ->
            isFocused.value = focusState.isFocused
        },
        singleLine = true,
        trailingIcon = if (isFocused.value) {
            {
                IconButton(onClick = { onTextChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "クリア")
                }
            }
        } else {
            null
        },
    )
}
