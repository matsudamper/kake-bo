package net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen

import androidx.compose.runtime.Composable
import net.matsudamper.money.frontend.common.ui.layout.TextFieldType

@Composable
public expect fun FullScreenTextInput(
    title: String,
    onComplete: (String) -> Unit,
    canceled: () -> Unit,
    default: String,
    name: String = "",
    inputType: TextFieldType = TextFieldType.Text,
    isMultiline: Boolean = false,
    autocomplete: String? = null,
)
