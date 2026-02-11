package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog as Material3AlertDialog

@Composable
internal fun AlertDialog(
    title: @Composable () -> Unit,
    description: (@Composable () -> Unit)? = null,
    positiveButton: (@Composable () -> Unit)? = null,
    negativeButton: (@Composable () -> Unit)? = null,
    onClickPositive: () -> Unit,
    onClickNegative: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    Material3AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onClickPositive) {
                if (positiveButton != null) {
                    positiveButton()
                } else {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onClickNegative) {
                if (negativeButton != null) {
                    negativeButton()
                } else {
                    Text("CANCEL")
                }
            }
        },
        title = title,
        text = description,
    )
}
