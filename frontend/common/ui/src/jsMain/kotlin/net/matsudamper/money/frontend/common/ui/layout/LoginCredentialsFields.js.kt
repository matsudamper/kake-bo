package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.browser.document
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLLabelElement
import org.w3c.dom.events.Event

@OptIn(ExperimentalComposeUiApi::class)
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
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.bodySmall

    HtmlElementView(
        modifier = modifier
            .fillMaxWidth()
            .height(FIELD_HEIGHT),
        factory = {
            val form = document.createElement("form") as HTMLFormElement
            form.id = FORM_ID
            form.setAttribute("autocomplete", "on")
            form.style.apply {
                width = "100%"
                height = "100%"
                margin = "0"
                padding = "0"
                border = "none"
            }

            val usernameLabelElement = document.createElement("label") as HTMLLabelElement
            usernameLabelElement.htmlFor = USERNAME_INPUT_ID

            val usernameInput = document.createElement("input") as HTMLInputElement
            usernameInput.id = USERNAME_INPUT_ID
            usernameInput.name = "username"
            usernameInput.type = "text"
            usernameInput.autocomplete = "username"
            usernameInput.required = true
            usernameInput.style.display = "block"

            val passwordLabelElement = document.createElement("label") as HTMLLabelElement
            passwordLabelElement.htmlFor = PASSWORD_INPUT_ID

            val passwordInput = document.createElement("input") as HTMLInputElement
            passwordInput.id = PASSWORD_INPUT_ID
            passwordInput.name = "password"
            passwordInput.type = "password"
            passwordInput.autocomplete = "current-password"
            passwordInput.required = true
            passwordInput.style.display = "block"

            form.append(
                usernameLabelElement,
                usernameInput,
                passwordLabelElement,
                passwordInput,
            )
            form
        },
        update = { form ->
            val usernameLabelElement = form.querySelector("label[for='$USERNAME_INPUT_ID']") as HTMLLabelElement
            val usernameInput = form.querySelector("#$USERNAME_INPUT_ID") as HTMLInputElement
            val passwordLabelElement = form.querySelector("label[for='$PASSWORD_INPUT_ID']") as HTMLLabelElement
            val passwordInput = form.querySelector("#$PASSWORD_INPUT_ID") as HTMLInputElement

            usernameLabelElement.textContent = usernameLabel
            usernameLabelElement.style.apply {
                display = "block"
                marginBottom = "4px"
                color = colors.onSurfaceVariant.toCssColor()
                fontSize = "${labelStyle.fontSize.value}px"
                fontFamily = labelStyle.fontFamily?.toString() ?: "inherit"
            }

            passwordLabelElement.textContent = passwordLabel
            passwordLabelElement.style.apply {
                display = "block"
                marginTop = "8px"
                marginBottom = "4px"
                color = colors.onSurfaceVariant.toCssColor()
                fontSize = "${labelStyle.fontSize.value}px"
                fontFamily = labelStyle.fontFamily?.toString() ?: "inherit"
            }

            usernameInput.disabled = !enabled
            usernameInput.style.apply {
                boxSizing = "border-box"
                width = "100%"
                minHeight = "56px"
                padding = "16px 12px"
                borderRadius = "4px"
                border = "1px solid ${colors.outline.toCssColor()}"
                backgroundColor = colors.surface.toCssColor()
                color = colors.onSurface.toCssColor()
                fontSize = "${textStyle.fontSize.value}px"
                fontFamily = textStyle.fontFamily?.toString() ?: "inherit"
            }

            passwordInput.disabled = !enabled
            passwordInput.style.apply {
                boxSizing = "border-box"
                width = "100%"
                minHeight = "56px"
                padding = "16px 12px"
                borderRadius = "4px"
                border = "1px solid ${colors.outline.toCssColor()}"
                backgroundColor = colors.surface.toCssColor()
                color = colors.onSurface.toCssColor()
                fontSize = "${textStyle.fontSize.value}px"
                fontFamily = textStyle.fontFamily?.toString() ?: "inherit"
            }

            if (usernameInput.value != username) {
                usernameInput.value = username
            }
            if (passwordInput.value != password) {
                passwordInput.value = password
            }

            usernameInput.oninput = { event ->
                val value = readInputValue(event)
                if (value != username) {
                    onUsernameChange(value)
                }
            }
            usernameInput.onchange = { event ->
                val value = readInputValue(event)
                if (value != username) {
                    onUsernameChange(value)
                }
            }

            passwordInput.oninput = { event ->
                val value = readInputValue(event)
                if (value != password) {
                    onPasswordChange(value)
                }
            }
            passwordInput.onchange = { event ->
                val value = readInputValue(event)
                if (value != password) {
                    onPasswordChange(value)
                }
            }

            form.onsubmit = { event ->
                event.preventDefault()
                if (enabled) {
                    onSubmit()
                }
            }
        },
    )
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
private val FIELD_HEIGHT = 156.dp
