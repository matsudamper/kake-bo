package net.matsudamper.money.frontend.common.ui.screen.importedmail.root

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.immutableListOf
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.ui.layout.MultilineTextField

@Composable
internal fun AiParsePromptDialog(
    uiState: MailScreenUiState.AiParsePromptDialog,
) {
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
                    uiState.modelOptions.forEach { option ->
                        FilterChip(
                            selected = option.selected,
                            onClick = { option.event.onClick() },
                            label = { Text(option.label) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                FilterChip(
                    selected = uiState.thinkingSelected,
                    onClick = { uiState.event.onClickThinking() },
                    label = { Text("Thinking") },
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "追加ルール",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "サーバーのプロンプトより優先して適用されます",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(4.dp))
                MultilineTextField(
                    modifier = Modifier.fillMaxWidth(),
                    text = uiState.additionalRules,
                    onValueChange = { uiState.event.onChangeAdditionalRules(it) },
                    placeholder = "例: 商品明細は支出ごとに分けてください",
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { uiState.event.onClickClearAdditionalRules() },
                    enabled = uiState.clearAdditionalRulesButtonEnabled,
                ) {
                    Text("クリア")
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

@Composable
@Preview
private fun AiParsePromptDialogPreview() {
    AppRoot {
        AiParsePromptDialog(
            uiState = MailScreenUiState.AiParsePromptDialog(
                additionalRules = "- 商品明細は支出ごとに分けてください。",
                clearAdditionalRulesButtonEnabled = true,
                modelOptions = immutableListOf(
                    createPreviewModelOption(label = "Flash Lite", selected = true),
                    createPreviewModelOption(label = "Flash", selected = false),
                ),
                thinkingSelected = true,
                event = object : MailScreenUiState.AiParsePromptDialogEvent {
                    override fun onChangeAdditionalRules(additionalRules: String) {}
                    override fun onClickClearAdditionalRules() {}
                    override fun onClickThinking() {}
                    override fun onClickSend() {}
                    override fun onDismissRequest() {}
                },
            ),
        )
    }
}

private fun createPreviewModelOption(
    label: String,
    selected: Boolean,
): MailScreenUiState.AiParseModelOption {
    return MailScreenUiState.AiParseModelOption(
        label = label,
        selected = selected,
        event = object : MailScreenUiState.AiParseModelOptionEvent {
            override fun onClick() {}
        },
    )
}
