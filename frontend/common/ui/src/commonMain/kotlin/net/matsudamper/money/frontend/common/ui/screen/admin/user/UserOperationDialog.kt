package net.matsudamper.money.frontend.common.ui.screen.admin.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

public class UserOperationDialogState(
    public val userName: String,
    public val listener: Listener,
) {
    @Immutable
    public interface Listener {
        public fun onDismissUserMenu()
        public fun onClickReplacePassword()
        public fun onClickDeletePassword()
    }
}

@Composable
internal fun UserOperationDialog(
    uiState: UserOperationDialogState,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = { uiState.listener.onDismissUserMenu() },
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = uiState.userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = rememberCustomFontFamily(),
                    )
                    IconButton(onClick = { uiState.listener.onDismissUserMenu() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { uiState.listener.onClickReplacePassword() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "パスワード上書き",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    }
                    Button(
                        onClick = { uiState.listener.onClickDeletePassword() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "パスワード削除",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    }
                }
            }
        }
    }
}
