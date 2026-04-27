package net.matsudamper.money.frontend.feature.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.layout.TextField
import net.matsudamper.money.frontend.common.ui.layout.TextFieldType
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@Composable
internal fun AdminLoginScreen(uiState: AdminLoginScreenUiState) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 400.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
        ) {
            Column(
                Modifier
                    .padding(16.dp),
            ) {
                val textFieldTextStyle = MaterialTheme.typography.bodyMedium
                    .merge(
                        TextStyle(
                            fontFamily = rememberCustomFontFamily(),
                        ),
                    )
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = "管理画面",
                    fontFamily = rememberCustomFontFamily(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(24.dp))
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { uiState.listener.onPasswordChanged(it) },
                    text = uiState.password.text,
                    textStyle = textFieldTextStyle,
                    label = "Password",
                    maxLines = 1,
                    type = TextFieldType.Password,
                    autocomplete = "current-password",
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    modifier = Modifier.align(Alignment.End),
                    onClick = { uiState.listener.onClickLogin() },
                ) {
                    Text(
                        text = "ログイン",
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
            }
        }
    }
}
