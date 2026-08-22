package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.browser.document
import androidx.compose.material3.TextFieldDefaults as MaterialTextFieldDefaults
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLLabelElement
import org.w3c.dom.events.Event

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
    Column(modifier = modifier) {
        HiddenLoginForm(
            onSubmit = onSubmit,
            enabled = enabled,
        )
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HiddenLoginForm(
    onSubmit: () -> Unit,
    enabled: Boolean,
) {
    HtmlElementView(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.dp),
        factory = {
            val form = document.createElement("form") as HTMLFormElement
            form.id = FORM_ID
            form.setAttribute("autocomplete", "on")
            form.style.apply {
                display = "none"
                margin = "0"
                padding = "0"
                border = "none"
            }
            form
        },
        update = { form ->
            form.onsubmit = { event ->
                event.preventDefault()
                if (enabled) {
                    onSubmit()
                }
            }
        },
    )
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

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        MaterialTextFieldDefaults.DecorationBox(
            value = value,
            visualTransformation = visualTransformation,
            innerTextField = {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = displayText,
                    style = textStyle,
                    maxLines = 1,
                )
            },
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
            colors = TextFieldDefaults.colors(),
        )

        HtmlElementView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val container = document.createElement("div") as org.w3c.dom.HTMLElement
                container.style.apply {
                    position = "relative"
                    width = "100%"
                    height = "100%"
                    margin = "0"
                    padding = "0"
                    border = "none"
                }

                val labelElement = document.createElement("label") as HTMLLabelElement
                labelElement.htmlFor = inputId
                labelElement.textContent = label
                applyVisuallyHiddenStyle(labelElement)

                val input = document.createElement("input") as HTMLInputElement
                input.id = inputId
                input.name = inputName
                input.type = inputType
                input.autocomplete = autocomplete
                input.setAttribute("form", FORM_ID)
                input.required = true

                container.append(labelElement, input)
                container
            },
            update = { container ->
                val input = container.querySelector("#$inputId") as HTMLInputElement

                input.disabled = !enabled
                applyMinimalInputStyle(
                    input = input,
                    textStyle = textStyle,
                    caretColor = colors.onSurface,
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
}

private fun applyVisuallyHiddenStyle(labelElement: HTMLLabelElement) {
    labelElement.style.apply {
        position = "absolute"
        width = "1px"
        height = "1px"
        padding = "0"
        margin = "-1px"
        setProperty("overflow", "hidden")
        clip = "rect(0, 0, 0, 0)"
        border = "0"
    }
}

private fun applyMinimalInputStyle(
    input: HTMLInputElement,
    textStyle: TextStyle,
    caretColor: Color,
) {
    input.style.apply {
        boxSizing = "border-box"
        position = "absolute"
        top = "0"
        left = "0"
        width = "100%"
        height = "100%"
        margin = "0"
        padding = "0"
        border = "none"
        outline = "none"
        backgroundColor = "transparent"
        color = "transparent"
        setProperty("caret-color", caretColor.toCssColor())
        fontSize = "${textStyle.fontSize.value}px"
        fontFamily = textStyle.fontFamily?.toString() ?: "inherit"
        lineHeight = textStyle.lineHeight.toString()
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
