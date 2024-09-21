package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.runtime.Composable

@Composable
public actual fun BackHandler(
    enable: Boolean,
    block: () -> Unit,
) {
    androidx.activity.compose.BackHandler(enable) {
        block()
    }
}
