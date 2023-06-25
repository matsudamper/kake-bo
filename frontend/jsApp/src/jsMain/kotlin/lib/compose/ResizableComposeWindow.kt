@file:Suppress(
    "INVISIBLE_MEMBER",
    "INVISIBLE_REFERENCE",
    "EXPOSED_PARAMETER_TYPE",
)

package lib.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.ComposeWindow
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTitleElement

private const val CANVAS_ELEMENT_ID = "ComposeTarget" // Hardwired into ComposeWindow


@Suppress("FunctionName")
public fun ResizableComposeWindow(
    title: String = "Untitled",
    content: @Composable ComposeWindow.() -> Unit,
) {
    val htmlHeadElement = document.head!!

    fun HTMLElement.fillViewportSize() {
        setAttribute("width", "${window.innerWidth}")
        setAttribute("height", "${window.innerHeight}")
    }

    val htmlComposeTarget = document.getElementById("HtmlComposeTarget")!! as HTMLElement
    val canvas = (document.getElementById(CANVAS_ELEMENT_ID) as HTMLCanvasElement).apply {
        fillViewportSize()
    }

    ComposeWindow().apply {
        // WORKAROUND: ComposeWindow does not implement `setTitle(title)`
        val htmlTitleElement = (
                htmlHeadElement.getElementsByTagName("title").item(0)
                    ?: document.createElement("title").also { htmlHeadElement.appendChild(it) }
                ) as HTMLTitleElement
        htmlTitleElement.textContent = title

        setContent {
            val density = LocalDensity.current
            LaunchedEffect(Unit) {
                window.addEventListener(
                    "resize",
                    {
                        canvas.fillViewportSize()
                        htmlComposeTarget.fillViewportSize()

                        layer.layer.attachTo(canvas)
                        val scale = layer.layer.contentScale
                        layer.setSize(
                            (canvas.width * density.density / scale).toInt(),
                            (canvas.height * density.density / scale).toInt(),
                        )
                        layer.layer.needRedraw()
                    },
                )
            }
            content(this)
        }
    }
}
