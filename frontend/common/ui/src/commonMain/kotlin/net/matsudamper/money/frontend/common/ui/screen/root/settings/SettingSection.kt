package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            content()
        }
    }
}
