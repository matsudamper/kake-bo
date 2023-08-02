package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingSection(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            title()
        }
        Spacer(Modifier.height(8.dp))
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            content()
        }
    }
}
