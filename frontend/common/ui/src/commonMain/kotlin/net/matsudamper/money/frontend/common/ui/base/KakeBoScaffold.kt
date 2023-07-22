package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun KakeBoTopAppBar(
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    title: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        TopAppBar(
            modifier = Modifier.fillMaxWidth(),
            navigationIcon = navigationIcon,
            title = title,
        )
        Divider(modifier = Modifier.fillMaxWidth().height(1.dp))
    }
}
