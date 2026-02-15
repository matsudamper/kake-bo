package net.matsudamper.money.frontend.common.ui.screen.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.layout.html.text.input.HtmlTextInput
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddUserScreen(
    modifier: Modifier = Modifier,
    uiState: AdminAddUserUiState,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Text(
                text = "ユーザー追加",
                fontFamily = rememberCustomFontFamily(),
            )
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HtmlTextInput(
                modifier = Modifier.fillMaxWidth()
                    .height(48.dp),
                placeholder = "ユーザー名",
                onValueChange = { uiState.onChangeUserName(it.text) },
                type = KeyboardType.Text,
            )
            Spacer(modifier = Modifier.height(12.dp))
            HtmlTextInput(
                modifier = Modifier.fillMaxWidth()
                    .height(48.dp),
                placeholder = "パスワード",
                onValueChange = { uiState.onChangePassword(it.text) },
                type = KeyboardType.Password,
            )
            Text(
                "使用できる記号 !@#\$%^&*()_+-?<>,.",
                fontFamily = rememberCustomFontFamily(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                modifier = Modifier.align(Alignment.End),
                onClick = { uiState.onClickAddButton() },
            ) {
                Text(
                    text = "追加",
                    fontFamily = rememberCustomFontFamily(),
                )
            }
        }
    }
}
