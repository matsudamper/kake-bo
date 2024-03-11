package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
public fun SettingScaffold(
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    title: @Composable () -> Unit,
    menu: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val settingHorizontalPadding = 24.dp

    Column(
        modifier =
        modifier
            .fillMaxWidth(),
    ) {
        Row(
            modifier =
            Modifier
                .padding(horizontal = settingHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                Modifier
                    .weight(1f)
                    .padding(
                        horizontal = 18.dp,
                        vertical = 24.dp,
                    ),
            ) {
                ProvideTextStyle(titleStyle) {
                    title()
                }
            }
            menu()
        }
        Divider(
            modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = settingHorizontalPadding),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(
                modifier = Modifier,
            ) {
                val max = 700.dp
                val width = maxWidth

                val paddingValues =
                    remember(max, width) {
                        if (max > width) {
                            PaddingValues(
                                horizontal = settingHorizontalPadding + 24.dp,
                            )
                        } else {
                            PaddingValues(
                                horizontal = (width - max) / 2 + (settingHorizontalPadding + 24.dp),
                            )
                        }
                    }
                content(paddingValues)
            }
        }
    }
}
