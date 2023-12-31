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
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.marginBottom
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
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.HTMLElement

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
                        marginBottom(0.5.cssRem)
                    }
                },
            ) {
                Text(value.title)
            }
            val inputId = "id_$id"

            val inputAreaAttr: AttrBuilderContext<HTMLElement> = {
                style {
                    padding(0.5.cssRem)
                    width(100.percent - (0.5.cssRem * 2))
                    fontSize(1.cssRem)
                }
            }
            if (value.isMultiline) {
                TextArea(
                    attrs = {
                        id(inputId)
                        name(value.name)
                        style {
                            height(10.cssRem)
                        }
                        placeholder(value.title)
                        onChange {
                            text = it.value
                        }
                        inputAreaAttr()
                    },
                )
            } else {
                Input(
                    type = InputType.InputTypeWithStringValue(value.inputType),
                    attrs = {
                        id(inputId)
                        name(value.name)
                        style {}
                        placeholder(value.title)
                        onChange {
                            text = it.value
                        }
                        inputAreaAttr()
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
                        marginTop(0.5.cssRem)
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
                            width(1.cssRem)
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
