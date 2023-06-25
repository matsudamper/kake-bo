package net.matsudamper.money.frontend.common.ui.screen.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily
import net.matsudamper.money.frontend.common.ui.CustomColors
import net.matsudamper.money.frontend.common.ui.layout.html.text.HtmlTextInput
import net.matsudamper.money.frontend.common.uistate.LoginScreenUiState


@Composable
public fun LoginScreen(
    uiState: LoginScreenUiState,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CustomColors.backgroundColor,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 500.dp)
                    .width(IntrinsicSize.Min),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .height(24.dp),
                    fontFamily = rememberCustomFontFamily(),
                    color = MaterialTheme.colorScheme.onSurface,
                    text = "ログイン",
                )
                val textBoxModifier = Modifier
                    .widthIn(300.dp)
                    .height(24.dp)
                HtmlTextInput(
                    modifier = textBoxModifier,
                    onValueChange = {
                        uiState.listener.onUserNameChange(it)
                    },
                    placeholder = "user name",
                    type = KeyboardType.Text,
                )
                Spacer(modifier = Modifier.height(16.dp))
                HtmlTextInput(
                    modifier = textBoxModifier,
                    onValueChange = {
                        uiState.listener.onPasswordChange(it)
                    },
                    placeholder = "password",
                    type = KeyboardType.Password,
                )
                Row(
                    modifier = Modifier.align(Alignment.End),
                ) {
                    OutlinedButton(
                        onClick = { uiState.listener.onClickNavigateAdmin() },
                    ) {
                        Text(text = "Admin Login Page")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { uiState.listener.onClickLogin() },
                    ) {
                        Text(text = "Login")
                    }
                }
            }
        }
    }
}
