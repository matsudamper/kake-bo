package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp

@Composable
public fun ChangeableFieldRow(
    modifier: Modifier = Modifier,
    multiline: Boolean = false,
    title: String,
    onClickBody: (() -> Unit)? = null,
    onClickChange: () -> Unit,
    body: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onClickBody != null) {
                        Modifier.clickable { onClickBody() }
                    } else {
                        Modifier
                    },
                ),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                body()
            }
        }
        OutlinedButton(
            onClick = onClickChange,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            border = ButtonDefaults.outlinedButtonBorder(true).copy(
                brush = SolidColor(MaterialTheme.colorScheme.primary),
            ),
        ) {
            Text("変更", style = MaterialTheme.typography.labelMedium)
        }
    }
}
