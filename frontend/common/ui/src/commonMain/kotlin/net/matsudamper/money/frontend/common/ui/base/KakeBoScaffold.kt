package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.onClick
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun KakeBoTopAppBar(
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    onClickTitle: () -> Unit = {},
    title: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        TopAppBar(
            modifier = Modifier.fillMaxWidth(),
            navigationIcon = navigationIcon,
            title = {
                Box(modifier = Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        onClickTitle()
                    }
                ) {
                    title()
                }
            },
        )
        Divider(modifier = Modifier.fillMaxWidth().height(1.dp))
    }
}
