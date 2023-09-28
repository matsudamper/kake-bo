package lib.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInputContext
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.em
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.minus
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.paddingLeft
import org.jetbrains.compose.web.css.paddingRight
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.rgba
import org.jetbrains.compose.web.css.times
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.css.vw
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.HTMLButtonElement

@Composable
internal fun HtmlFullScreenTextInputContent(
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
            if (value.isMultiline) {
                TextArea(
                    attrs = {
                        id(inputId)
                        style {
                            width(100.percent)
                            height(10.em)
                        }
                        placeholder(value.title)
                        onChange {
                            text = it.value
                        }
                    },
                )
            } else {
                Input(
                    type = InputType.Text,
                    attrs = {
                        id(inputId)
                        style {
                            fontSize(1.1.em)
                            padding(0.2.em)
                            width(100.percent)
                        }
                        placeholder(value.title)
                        onChange {
                            text = it.value
                        }
                    },
                )
            }
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
                ConfirmButton(
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
                ConfirmButton(
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
