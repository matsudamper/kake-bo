package net.matsudamper.money.frontend.common.ui.screen.importedmail.root

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.FullScreenTextInput

@Composable
internal fun AiParsePromptDialog(
    uiState: MailScreenUiState.AiParsePromptDialog,
) {
    if (uiState.isPromptEditing) {
        FullScreenTextInput(
            title = "プロンプト",
            onComplete = { uiState.event.onCompletePromptEdit(it) },
            canceled = { uiState.event.onCancelPromptEdit() },
            default = uiState.prompt,
            isMultiline = true,
        )
        return
    }
    AlertDialog(
        onDismissRequest = { uiState.event.onDismissRequest() },
        title = { Text("AIで解析") },
        text = {
            Column {
                Text(
                    text = "モデル",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MailScreenUiState.AiParseModel.entries.forEach { model ->
                        FilterChip(
                            selected = model == uiState.selectedModel,
                            onClick = { uiState.event.onSelectModel(model) },
                            label = { Text(model.displayName) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "プロンプト",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "レスポンスのJSON定義は自動で付与されます",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                        text = uiState.prompt,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { uiState.event.onClickEditPrompt() }) {
                        Text("編集")
                    }
                    OutlinedButton(onClick = { uiState.event.onClickResetPrompt() }) {
                        Text("初期値に戻す")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { uiState.event.onClickSend() }) {
                Text("解析")
            }
        },
        dismissButton = {
            TextButton(onClick = { uiState.event.onDismissRequest() }) {
                Text("キャンセル")
            }
        },
    )
}
