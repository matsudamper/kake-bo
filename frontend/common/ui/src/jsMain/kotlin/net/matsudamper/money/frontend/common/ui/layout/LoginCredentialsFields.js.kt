package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.browser.document
import androidx.compose.material3.TextFieldDefaults as MaterialTextFieldDefaults
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event

private val CredentialFieldMinHeight = 56.dp
private val CredentialFieldHorizontalPadding = 16.dp

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
public actual fun LoginCredentialsFields(
    username: String,
    password: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    usernameLabel: String,
    passwordLabel: String,
    textStyle: TextStyle,
    modifier: Modifier,
    enabled: Boolean,
) {
    HiddenLoginForm(
        onSubmit = onSubmit,
        enabled = enabled,
    )
    Column(modifier = modifier) {
        HtmlCredentialField(
            label = usernameLabel,
            value = username,
            onValueChange = onUsernameChange,
            inputId = USERNAME_INPUT_ID,
            inputName = "username",
            inputType = "text",
            autocomplete = "username",
            textStyle = textStyle,
            enabled = enabled,
            isPassword = false,
        )
        Spacer(modifier = Modifier.height(8.dp))
        HtmlCredentialField(
            label = passwordLabel,
            value = password,
            onValueChange = onPasswordChange,
            inputId = PASSWORD_INPUT_ID,
            inputName = "password",
            inputType = "password",
            autocomplete = "current-password",
            textStyle = textStyle,
            enabled = enabled,
            isPassword = true,
        )
    }
}

@Composable
private fun HiddenLoginForm(
    onSubmit: () -> Unit,
    enabled: Boolean,
) {
    DisposableEffect(enabled) {
        val form = document.getElementById(FORM_ID) as HTMLFormElement?
            ?: run {
                val created = document.createElement("form") as HTMLFormElement
                created.id = FORM_ID
                created.setAttribute("autocomplete", "on")
                created.style.display = "none"
                document.body?.appendChild(created)
                created
            }
        form.onsubmit = { event ->
            event.preventDefault()
            if (enabled) {
                onSubmit()
            }
        }
        onDispose {
            form.onsubmit = null
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun HtmlCredentialField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    inputId: String,
    inputName: String,
    inputType: String,
    autocomplete: String,
    textStyle: TextStyle,
    enabled: Boolean,
    isPassword: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val visualTransformation = if (isPassword) {
        PasswordVisualTransformation()
    } else {
        VisualTransformation.None
    }
    val displayText = visualTransformation.filter(AnnotatedString(value)).text
    val textFieldColors = TextFieldDefaults.colors()

    SubcomposeLayout(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = CredentialFieldMinHeight),
    ) { constraints ->
        val decorationPlaceable = subcompose("decoration") {
            MaterialTextFieldDefaults.DecorationBox(
                value = value,
                visualTransformation = visualTransformation,
                innerTextField = {},
                label = {
                    Text(label)
                },
                placeholder = null,
                leadingIcon = null,
                trailingIcon = null,
                prefix = null,
                suffix = null,
                supportingText = null,
                shape = MaterialTextFieldDefaults.shape,
                singleLine = true,
                enabled = enabled,
                isError = false,
                interactionSource = interactionSource,
                colors = textFieldColors,
            )
        }.map { measurable ->
            measurable.measure(constraints)
        }.first()

        val overlayWidth = decorationPlaceable.width
        val overlayHeight = decorationPlaceable.height
        val overlayWidthDp = overlayWidth.toDp()
        val overlayHeightDp = overlayHeight.toDp()

        val overlayPlaceable = subcompose("overlay") {
            Box(
                modifier = Modifier
                    .width(overlayWidthDp)
                    .height(overlayHeightDp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CredentialFieldHorizontalPadding),
                    text = displayText,
                    style = textStyle,
                    maxLines = 1,
                )
                HtmlElementView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        val input = document.createElement("input") as HTMLInputElement
                        input.id = inputId
                        input.name = inputName
                        input.type = inputType
                        input.autocomplete = autocomplete
                        input.setAttribute("form", FORM_ID)
                        input.setAttribute("aria-label", label)
                        input.required = true
                        input
                    },
                    update = { input ->
                        input.disabled = !enabled
                        applyMinimalInputStyle(
                            input = input,
                            textStyle = textStyle,
                            caretColor = colors.onSurface,
                            horizontalPaddingPx = with(density) {
                                CredentialFieldHorizontalPadding.roundToPx()
                            },
                        )

                        if (input.value != value) {
                            input.value = value
                        }

                        input.oninput = { event ->
                            val newValue = readInputValue(event)
                            if (newValue != value) {
                                onValueChange(newValue)
                            }
                        }
                        input.onchange = { event ->
                            val newValue = readInputValue(event)
                            if (newValue != value) {
                                onValueChange(newValue)
                            }
                        }
                    },
                )
            }
        }.map { measurable ->
            measurable.measure(
                Constraints.fixed(
                    width = overlayWidth,
                    height = overlayHeight,
                ),
            )
        }.first()

        layout(overlayWidth, overlayHeight) {
            decorationPlaceable.placeRelative(0, 0)
            overlayPlaceable.placeRelative(0, 0)
        }
    }
}

private fun applyMinimalInputStyle(
    input: HTMLInputElement,
    textStyle: TextStyle,
    caretColor: Color,
    horizontalPaddingPx: Int,
) {
    val clientHeight = input.clientHeight
    input.style.apply {
        boxSizing = "border-box"
        width = "100%"
        height = "100%"
        margin = "0"
        padding = "0 ${horizontalPaddingPx}px"
        border = "none"
        outline = "none"
        backgroundColor = "transparent"
        color = "transparent"
        setProperty("appearance", "none")
        setProperty("-webkit-appearance", "none")
        setProperty("caret-color", caretColor.toCssColor())
        fontSize = "${textStyle.fontSize.value}px"
        fontFamily = textStyle.fontFamily?.toString() ?: "inherit"
        if (clientHeight > 0) {
            lineHeight = "${clientHeight}px"
        } else {
            lineHeight = textStyle.lineHeight.toString()
        }
        setProperty("-webkit-text-fill-color", "transparent")
    }
}

private fun readInputValue(event: Event): String {
    val target = event.target
    return (target as? HTMLInputElement)?.value ?: ""
}

private fun Color.toCssColor(): String {
    val argb = toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return "rgb($r, $g, $b)"
}

private const val FORM_ID = "login-credentials-form"
private const val USERNAME_INPUT_ID = "login-username"
private const val PASSWORD_INPUT_ID = "login-password"
