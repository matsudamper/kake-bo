package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@Composable
internal fun SettingListMenuItemButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    text: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(horizontal = 8.dp)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        ProvideTextStyle(
            titleStyle.copy(
                fontFamily = rememberCustomFontFamily(),
            ),
        ) {
            text()
        }
    }
}
