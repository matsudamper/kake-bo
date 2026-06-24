package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.ui.AppRoot

/**
 * Preview-only wrapper to align colors with RootScreenScaffold/KakeboScaffold.
 */
@Composable
internal fun HomePreviewSurface(
    isDarkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    AppRoot(isDarkTheme = isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            content()
        }
    }
}
