package net.matsudamper.money.frontend.common.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily
import net.matsudamper.money.frontend.common.ui.layout.html.text.input.HtmlTextInput

@Composable
public fun AdminLoginScreen(
    uiState: AdminLoginScreenUiState,
) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 500.dp)
                .width(IntrinsicSize.Min),
            shape = RoundedCornerShape(4.dp),
        ) {
            Column(
                Modifier
                    .padding(16.dp),
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = "管理画面",
                    fontFamily = rememberCustomFontFamily(),
                )
                Spacer(Modifier.height(24.dp))
                HtmlTextInput(
                    modifier = Modifier.width(300.dp)
                        .height(24.dp),
                    placeholder = "password",
                    type = KeyboardType.Password,
                    onValueChange = {
                        uiState.onChangePassword(it)
                    },
                )
                Button(
                    modifier = Modifier.align(Alignment.End),
                    onClick = { uiState.onClickLogin() },
                ) {
                    Text(text = "Login")
                }
            }
        }
    }
}
