package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
public fun LoadingErrorContent(
    modifier: Modifier = Modifier,
    onClickRetry: () -> Unit,
    contentAlignment: Alignment = Alignment.Center,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = contentAlignment,
    ) {
        Column(
            modifier = Modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "エラーが発生しました",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onClickRetry,
            ) {
                Text(text = "再読み込み")
            }
        }
    }
}
