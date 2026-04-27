package net.matsudamper.money.frontend.feature.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.Strings
import net.matsudamper.money.frontend.common.ui.layout.TextField
import net.matsudamper.money.frontend.common.ui.layout.TextFieldType
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@Composable
internal fun AddUserScreen(
    modifier: Modifier = Modifier,
    uiState: AdminAddUserUiState,
) {
    Card(
        modifier = modifier
            .padding(24.dp)
            .widthIn(max = 480.dp)
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "ユーザー追加",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = rememberCustomFontFamily(),
            )
            Spacer(modifier = Modifier.height(20.dp))
            val textFieldTextStyle = MaterialTheme.typography.bodyMedium
                .merge(
                    TextStyle(
                        fontFamily = rememberCustomFontFamily(),
                    ),
                )
            TextField(
                modifier = Modifier.fillMaxWidth(),
                text = uiState.userName.text,
                textStyle = textFieldTextStyle,
                label = "User Name",
                maxLines = 1,
                onValueChange = { uiState.listener.onChangeUserName(it) },
                autocomplete = "username",
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextField(
                modifier = Modifier.fillMaxWidth(),
                text = uiState.password.text,
                textStyle = textFieldTextStyle,
                label = "Password",
                maxLines = 1,
                onValueChange = { uiState.listener.onChangePassword(it) },
                type = TextFieldType.Password,
                autocomplete = "new-password",
            )
            Text(
                Strings.PASSWORD_ALLOW_SYMBOLS_DESCRIPTION,
                fontFamily = rememberCustomFontFamily(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                modifier = Modifier.align(Alignment.End),
                onClick = { uiState.listener.onClickAddButton() },
            ) {
                Text(
                    text = "追加",
                    fontFamily = rememberCustomFontFamily(),
                )
            }
        }
    }
}
