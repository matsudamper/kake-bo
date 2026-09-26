package net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
) {
    var text by remember { mutableStateOf(default) }
    Dialog(
        onDismissRequest = canceled,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f),
            ) {
                Text(
                    text = title,
                    color = Color.White,
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = canceled) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { onComplete(text) }) {
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
