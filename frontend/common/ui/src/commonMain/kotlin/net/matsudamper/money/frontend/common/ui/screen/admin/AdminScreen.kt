package net.matsudamper.money.frontend.common.ui.screen.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminRootScreen(
    modifier: Modifier = Modifier,
    uiState: AdminRootScreenUiState,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "管理画面",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
                actions = {
                    TextButton(
                        onClick = uiState.listener::onClickLogout,
                    ) {
                        Text(
                            text = "ログアウト",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
        ) {
            RootSettingItem(
                text = {
                    Text(
                        text = "ユーザー追加",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
                onClick = { uiState.listener.onClickAddUser() },
            )
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun RootSettingItem(
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth()
            .clickable {
                onClick()
            }
            .padding(12.dp),
    ) {
        text()
    }
}
