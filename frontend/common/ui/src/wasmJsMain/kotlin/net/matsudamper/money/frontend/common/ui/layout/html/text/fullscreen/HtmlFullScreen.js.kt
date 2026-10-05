package net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import androidx.compose.ui.window.Dialog
import kotlinx.browser.document
import net.matsudamper.money.frontend.common.ui.layout.TextFieldType
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement

@OptIn(ExperimentalComposeUiApi::class)
@Composable
public actual fun FullScreenTextInput(
    title: String,
    onComplete: (String) -> Unit,
    canceled: () -> Unit,
    default: String,
    name: String,
    inputType: TextFieldType,
    isMultiline: Boolean,
    autocomplete: String?,
    errorMessage: String?,
) {
    var text by remember { mutableStateOf(default) }
    Dialog(onDismissRequest = canceled) {
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
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                HtmlElementView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isMultiline) 160.dp else 48.dp),
                    factory = {
                        createTextInputElement(
                            isMultiline = isMultiline,
                            inputType = inputType.htmlString(),
                            name = name,
                            placeholder = title,
                            autocomplete = autocomplete,
                            initialValue = default,
                            onValueChange = { text = it },
                        )
                    },
                )
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
                    TextButton(onClick = canceled) {
                        Text("CANCEL")
                    }
                    TextButton(onClick = { onComplete(text) }) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

private fun createTextInputElement(
    isMultiline: Boolean,
    inputType: String,
    name: String,
    placeholder: String,
    autocomplete: String?,
    initialValue: String,
    onValueChange: (String) -> Unit,
): HTMLElement {
    val element = if (isMultiline) {
        val textArea = document.createElement("textarea") as HTMLTextAreaElement
        textArea.value = initialValue
        textArea.oninput = { onValueChange(textArea.value) }
        textArea
    } else {
        val input = document.createElement("input") as HTMLInputElement
        input.type = inputType
        input.value = initialValue
        input.oninput = { onValueChange(input.value) }
        input
    }
    element.setAttribute("name", name)
    element.setAttribute("placeholder", placeholder)
    if (autocomplete != null) {
        element.setAttribute("autocomplete", autocomplete)
    }
    element.style.apply {
        boxSizing = "border-box"
        width = "100%"
        height = "100%"
        padding = "8px"
        fontSize = "16px"
    }
    return element
}
