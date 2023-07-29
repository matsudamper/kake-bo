package lib.compose

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntSize
import kotlinx.browser.document
import kotlinx.coroutines.flow.StateFlow
import lib.js.ResizeObserver
import net.matsudamper.money.frontend.common.ui.layout.html.html.LocalHtmlRenderContext
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.LocalHtmlFullScreenTextInputContext
import net.matsudamper.money.frontend.common.ui.layout.html.text.input.LocalHtmlTextInputContext
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.em
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.outline
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Canvas
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Iframe
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable

@Suppress("FunctionName")
internal fun JsCompose(
    composeSize: StateFlow<IntSize>,
) {
    renderComposable(rootElementId = "ComposeTargetContainer") {
        val htmlRenderContextState by LocalHtmlRenderContext.current.stateFlow.collectAsState()
        val htmlFullScreenTextInputContextState by LocalHtmlFullScreenTextInputContext.current.stateFlow.collectAsState()

        val hasFullScreenOverlay by remember {
            derivedStateOf {
                htmlRenderContextState.isNotEmpty() ||
                    htmlFullScreenTextInputContextState.isNotEmpty()
            }
        }

        Canvas(
            attrs = {
                id("ComposeTarget")
                style {
                    width(100.percent)
                    height(100.percent)
                    position(Position.Absolute)
                    outline("none")
                    if (hasFullScreenOverlay) {
                        property(
                            "pointer-events",
                            "none",
                        )
                    }
                }
            },
        )
    }

    renderComposable(rootElementId = "HtmlComposeTarget") {
        val widthDensityState: MutableState<Float?> = remember { mutableStateOf(null) }
        val heightDensityState: MutableState<Float?> = remember { mutableStateOf(null) }
        LaunchedEffect(Unit) {
            val htmlComposeTarget = document.getElementById("ComposeTarget")!!
            ResizeObserver { entries, _ ->
                val target = entries[0].target

                widthDensityState.value = run {
                    val jsWidth = target.scrollWidth.toFloat()
                    val composeWidth = composeSize.value.width.toFloat()

                    if (jsWidth > 0 && composeWidth > 0) {
                        jsWidth / composeWidth
                    } else {
                        1f
                    }
                }
                heightDensityState.value = run {
                    val jsSize = target.scrollHeight.toFloat()
                    val composeHeight = composeSize.value.height.toFloat()

                    if (jsSize > 0 && composeHeight > 0) {
                        jsSize / composeHeight
                    } else {
                        1f
                    }
                }
            }.observe(htmlComposeTarget)
        }

        val htmlRenderContextState by LocalHtmlRenderContext.current.stateFlow.collectAsState()
        val htmlFullScreenTextInputContextState by LocalHtmlFullScreenTextInputContext.current.stateFlow.collectAsState()
        val htmlTextInputContextState by LocalHtmlTextInputContext.current.stateFlow.collectAsState()

        run {
            val heightDensity = heightDensityState.value
            val widthDensity = widthDensityState.value
            if (heightDensity != null && widthDensity != null) {
                htmlTextInputContextState.forEach { entry ->
                    HtmlTextInput(
                        id = "id_${entry.key}",
                        textState = entry.value,
                        heightDensity = heightDensity,
                        widthDensity = widthDensity,
                    )
                }
            }
        }

        run {
            htmlRenderContextState.toList().lastOrNull()?.also { (id, value) ->
                Div(
                    attrs = {
                        style {
                            display(DisplayStyle.Flex)
                            height(100.percent)
                            flexDirection(FlexDirection.Column)
                            overflow("hidden")
                            property(
                                "pointer-events",
                                "all",
                            )
                        }
                    },
                ) {
                    Iframe(
                        attrs = {
                            style {
                                flexGrow(1)
                                backgroundColor(Color.white)
                                color(Color.black)
                                display(DisplayStyle.Block)
                            }
                            attr("frameboader", "0")
                            attr("srcdoc", value.html)
                            attr("sandbox", "")
                        },
                    )
                    Button(
                        attrs = {
                            style {
                                height(4.em)
                                padding(1.em)
                                width(100.percent)
                            }
                            onClick {
                                value.onDismissRequest()
                            }
                        },
                    ) {
                        Text("閉じる")
                    }
                }
            }
        }

        run {
            htmlFullScreenTextInputContextState.toList().lastOrNull()?.also { (id, value) ->
                HtmlFullScreenTextInputContent(
                    id = id,
                    value = value,
                )
            }
        }
    }
}
