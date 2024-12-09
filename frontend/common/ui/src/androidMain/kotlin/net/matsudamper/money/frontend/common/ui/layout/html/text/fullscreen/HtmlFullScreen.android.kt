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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.matsudamper.money.frontend.common.ui.layout.TextFieldType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public actual fun FullScreenTextInput(
    title: String,
    onComplete: (String) -> Unit,
    canceled: () -> Unit,
    default: String,
    name: String,
    inputType: TextFieldType,
    isMultiline: Boolean,
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
                BasicTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        .padding(8.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.merge(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    state = state,
                )
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
