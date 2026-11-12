package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

@Composable
public fun MultilineTextField(
    text: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        modifier = modifier,
        value = text,
        onValueChange = onValueChange,
        textStyle = textStyle,
        placeholder = { Text(placeholder) },
        minLines = 4,
        maxLines = 8,
    )
}
