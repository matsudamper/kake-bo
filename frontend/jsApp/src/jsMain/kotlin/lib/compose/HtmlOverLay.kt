package lib.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import lib.js.ResizeObserver
import net.matsudamper.money.frontend.common.ui.layout.html.text.LocalHtmlTextContext
import net.matsudamper.money.frontend.common.ui.layout.html.text.LocalHtmlTextInputContext
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInputContext
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.LocalHtmlFullScreenTextInputContext
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.em
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.minHeight
import org.jetbrains.compose.web.css.minus
import org.jetbrains.compose.web.css.paddingLeft
import org.jetbrains.compose.web.css.paddingRight
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.rgba
import org.jetbrains.compose.web.css.times
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.css.vw
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable

@Suppress("FunctionName")
fun HtmlTextOverLay(
    composeSize: StateFlow<IntSize>,
) {
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

        run {
            val htmlTextInputContext = LocalHtmlTextInputContext.current

            val heightDensity = heightDensityState.value
            val widthDensity = widthDensityState.value
            if (heightDensity != null && widthDensity != null) {
                htmlTextInputContext.stateFlow.collectAsState().value.forEach { entry ->
                    val positionInWindow = entry.value.position
                    val id = remember(entry.key) { entry.key.toString() }

                    val coroutineScope = rememberCoroutineScope()
                    DisposableEffect(id, entry.value.sizeCallback) {
                        val resizeObserver = ResizeObserver { resizeEntries, _ ->
                            for (resizeEntry in resizeEntries) {
                                val rect = resizeEntry.contentRect
                                entry.value.sizeCallback(
                                    Size(
                                        (rect.width / widthDensity).toFloat(),
                                        (rect.height / heightDensity).toFloat(),
                                    ),
                                )
                            }
                        }

                        coroutineScope.launch {
                            while (isActive) {
                                val targetElement = document.getElementById(id)
                                if (targetElement == null) {
                                    delay(10)
                                    continue
                                }
                                resizeObserver.observe(targetElement)
                                break
                            }
                        }

                        onDispose {
                            resizeObserver.disconnect()
                        }
                    }

                    Input(
                        type = InputType.Text,
                        attrs = {
                            id(id)
                            onChange {
                                entry.value.textCallback(it.value)
                            }
                            type(InputType.InputTypeWithUnitValue(entry.value.type ?: "password"))
                            inputMode(
                                entry.value.type.toString(),
                            )
                            placeholder(entry.value.placeholder)
                            style {
                                property(
                                    "pointer-events",
                                    "all",
                                )
                                val color = entry.value.color
                                if (color != null) {
                                    color(rgba(color.red * 255, color.green * 255, color.blue * 255, color.alpha * 255))
                                }
                                position(Position.Absolute)
                                top((positionInWindow.y.px) * heightDensity)
                                left((positionInWindow.x.px) * widthDensity)
                                minHeight((entry.value.maxHeight ?: Int.MAX_VALUE).px * heightDensity)
                                width(((entry.value.width ?: Int.MAX_VALUE).px) * widthDensity)
                            }
                        },
                    )
                }
            }
        }

        run {
            val htmlTextContext = LocalHtmlTextContext.current

            val heightDensity = heightDensityState.value
            val widthDensity = widthDensityState.value
            if (heightDensity != null && widthDensity != null) {
                htmlTextContext.stateFlow.collectAsState().value.forEach { entry ->
                    val positionInWindow = entry.value.position
                    val id = remember(entry.key) { entry.key.toString() }
                    LaunchedEffect(id) {
                        val targetElement = document.getElementById(id) ?: return@LaunchedEffect

                        val resizeObserver = ResizeObserver { resizeEntries, _ ->
                            for (resizeEntry in resizeEntries) {
                                val rect = resizeEntry.contentRect
                                entry.value.sizeCallback(Size((rect.width / widthDensity).toFloat(), (rect.height / heightDensity).toFloat()))
                            }
                        }
                        resizeObserver.observe(targetElement)
                    }
                    Div(
                        attrs = {
                            id(id)
                            style {
                                property(
                                    "pointer-events",
                                    "all",
                                )
                                val color = entry.value.color
                                if (color != null) {
                                    color(rgba(color.red * 255, color.green * 255, color.blue * 255, color.alpha * 255))
                                }

                                position(Position.Absolute)
                                top((positionInWindow.y.px) * heightDensity)
                                left((positionInWindow.x.px) * widthDensity)
                                maxHeight((entry.value.maxHeight ?: Int.MAX_VALUE).px * heightDensity)
                                maxWidth(((entry.value.maxWidth ?: Int.MAX_VALUE).px) * widthDensity)
                            }
                        },
                    ) {
                        Text(entry.value.text)
                    }
                }
            }
        }

        run {
            LocalHtmlFullScreenTextInputContext.current.stateFlow.collectAsState().value.toList().lastOrNull()?.also { (id, value) ->
                HtmlFullScreenTextInputContent(
                    id = id,
                    value = value,
                )
            }
        }
    }
}

@Composable
private fun HtmlFullScreenTextInputContent(
    id: String,
    value: HtmlFullScreenTextInputContext.TextState,
) {
    var text by remember { mutableStateOf("") }
    Div(
        attrs = {
            style {
                width(100.vw)
                height(100.vh)
                backgroundColor(rgba(0, 0, 0, 0.8))
                property(
                    "pointer-events",
                    "all",
                )
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                justifyContent(JustifyContent.Center)
                flexDirection(FlexDirection.Column)
            }
        },
    ) {
        Div(
            attrs = {
                style {
                    width(100.percent - (5.percent * 2))
                    paddingLeft(5.percent)
                    paddingRight(5.percent)
                }
            },
        ) {
            Div(
                attrs = {
                    style {
                        color(Color.white)
                    }
                },
            ) {
                Text(value.title)
            }
            val inputId = "id_$id"
            Input(
                type = InputType.Text,
                attrs = {
                    id(inputId)
                    style {
                        width(100.percent)
                    }
                    placeholder(value.title)
                    onChange {
                        text = it.value
                    }
                },
            )
            LaunchedEffect(value.default) {
                val input = document.querySelector("#$inputId")!!
                text = value.default
                input.asDynamic().value = value.default
            }

            Div(
                attrs = {
                    style {
                        marginTop(0.5.em)
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Row)
                        alignItems(AlignItems.Center)
                        justifyContent(JustifyContent.End)
                    }
                },
            ) {
                Button(
                    attrs = {
                        onClick {
                            value.canceled()
                        }
                    },
                ) {
                    Text("Cancel")
                }
                Div(
                    attrs = {
                        style {
                            width(1.em)
                        }
                    },
                )
                Button(
                    attrs = {

                        onClick {
                            value.textComplete(text)
                        }
                    },
                ) {
                    Text("OK")
                }
            }
        }
    }
}