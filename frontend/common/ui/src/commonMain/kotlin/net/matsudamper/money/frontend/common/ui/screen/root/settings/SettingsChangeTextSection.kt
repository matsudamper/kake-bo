package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@Composable
internal fun SettingsChangeTextSection(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    onClickChange: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            CompositionLocalProvider(
                LocalTextStyle provides
                    LocalTextStyle.current.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            ) {
                title()
            }
            Spacer(Modifier.height(2.dp))
            CompositionLocalProvider(
                LocalTextStyle provides
                    LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                    ),
            ) {
                text()
            }
        }
        OutlinedButton(
            onClick = { onClickChange() },
        ) {
            Text(
                text = "変更",
                fontFamily = rememberCustomFontFamily(),
            )
        }
    }
}
