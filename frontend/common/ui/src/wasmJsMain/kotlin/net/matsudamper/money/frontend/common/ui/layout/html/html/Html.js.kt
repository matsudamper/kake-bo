package net.matsudamper.money.frontend.common.ui.layout.html.html

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.browser.document
import org.w3c.dom.HTMLIFrameElement

@OptIn(ExperimentalComposeUiApi::class)
@Composable
public actual fun Html(html: String, onDismissRequest: () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HtmlElementView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = {
                    val iframe = document.createElement("iframe") as HTMLIFrameElement
                    iframe.setAttribute("sandbox", "")
                    iframe.style.apply {
                        display = "block"
                        width = "100%"
                        height = "100%"
                        border = "none"
                        backgroundColor = "white"
                        color = "black"
                    }
                    iframe
                },
                update = { iframe ->
                    if (iframe.getAttribute("srcdoc") != html) {
                        iframe.setAttribute("srcdoc", html)
                    }
                },
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RectangleShape,
                onClick = onDismissRequest,
            ) {
                Text("閉じる")
            }
        }
    }
}
