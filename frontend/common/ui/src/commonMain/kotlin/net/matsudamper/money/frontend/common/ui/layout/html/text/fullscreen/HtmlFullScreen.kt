package net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.matsudamper.money.frontend.common.ui.layout.TextFieldType

@Composable
public fun FullScreenTextInput(
    title: String,
    onComplete: (String) -> Unit,
    canceled: () -> Unit,
    default: String,
    inputType: TextFieldType = TextFieldType.Text,
    isMultiline: Boolean = false,
    errorMessage: String? = null,
) {
    val state = rememberTextFieldState(initialText = default)
    Dialog(onDismissRequest = { canceled() }) {
        Card(
            modifier = Modifier
                .widthIn(max = 1000.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp,
            ),
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp),
            ) {
                Text(
                    modifier = Modifier,
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                val textFieldModifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = title }
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    .padding(8.dp)
                val textStyle = MaterialTheme.typography.bodyLarge.merge(
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface)
                when (inputType) {
                    TextFieldType.Text -> {
                        BasicTextField(
                            modifier = textFieldModifier,
                            textStyle = textStyle,
                            cursorBrush = cursorBrush,
                            state = state,
                            lineLimits = if (isMultiline) {
                                TextFieldLineLimits.MultiLine(maxHeightInLines = MultilineMaxHeightInLines)
                            } else {
                                TextFieldLineLimits.SingleLine
                            },
                        )
                    }

                    TextFieldType.Password -> {
                        BasicSecureTextField(
                            modifier = textFieldModifier,
                            textStyle = textStyle,
                            cursorBrush = cursorBrush,
                            state = state,
                        )
                    }
                }
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { canceled() },
                    ) {
                        Text("CANCEL")
                    }
                    TextButton(
                        onClick = { onComplete(state.text.toString()) },
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

private const val MultilineMaxHeightInLines = 8
