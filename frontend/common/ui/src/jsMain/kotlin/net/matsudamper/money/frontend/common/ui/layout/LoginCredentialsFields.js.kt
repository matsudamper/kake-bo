package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.browser.document
import androidx.compose.material3.TextFieldDefaults as MaterialTextFieldDefaults
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event

private val InputLineHeight = 24.dp

private class FocusInteractionHolder {
    var focus: FocusInteraction.Focus? = null
}

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
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val focusInteractionHolder = remember { FocusInteractionHolder() }
    val clickInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = clickInteractionSource,
                indication = null,
                enabled = enabled,
            ) {
                (document.getElementById(inputId) as? HTMLInputElement)?.focus()
            },
    ) {
        MaterialTextFieldDefaults.DecorationBox(
            value = value,
            visualTransformation = VisualTransformation.None,
            innerTextField = {
                HtmlElementView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(InputLineHeight),
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
                        applyInputStyle(
                            input = input,
                            fontSizeSp = textStyle.fontSize.value,
                            textColor = colors.onSurface,
                        )
                        bindFocusHandlers(
                            input = input,
                            interactionSource = interactionSource,
                            focusInteractionHolder = focusInteractionHolder,
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
    }
}

private fun bindFocusHandlers(
    input: HTMLInputElement,
    interactionSource: MutableInteractionSource,
    focusInteractionHolder: FocusInteractionHolder,
) {
    input.onfocus = {
        if (focusInteractionHolder.focus == null) {
            val focus = FocusInteraction.Focus()
            focusInteractionHolder.focus = focus
            interactionSource.tryEmit(focus)
        }
    }
    input.onblur = {
        val focus = focusInteractionHolder.focus
        if (focus != null) {
            interactionSource.tryEmit(FocusInteraction.Unfocus(focus))
            focusInteractionHolder.focus = null
        }
    }
}

private fun applyInputStyle(
    input: HTMLInputElement,
    fontSizeSp: Float,
    textColor: Color,
) {
    input.style.apply {
        boxSizing = "border-box"
        display = "block"
        width = "100%"
        height = "100%"
        margin = "0"
        padding = "0"
        border = "none"
        outline = "none"
        backgroundColor = "transparent"
        color = textColor.toCssColor()
        setProperty("appearance", "none")
        setProperty("-webkit-appearance", "none")
        fontSize = "${fontSizeSp}px"
        lineHeight = "normal"
        fontFamily = "inherit"
        letterSpacing = "normal"
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
