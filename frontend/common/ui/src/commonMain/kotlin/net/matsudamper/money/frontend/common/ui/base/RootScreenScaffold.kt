package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

public enum class RootScreenTab {
    Home,
    List,
    Add,
    Settings,
}

@Composable
internal fun RootScreenScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    windowInsets: PaddingValues,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        KakeboScaffold(
            modifier = Modifier.fillMaxWidth(),
            topBar = topBar,
            windowInsets = windowInsets,
            snackbarHost = {
                MySnackBarHost(
                    hostState = snackbarHostState,
                )
            },
        ) {
            Box(
                modifier = Modifier.padding(it),
            ) {
                content()
            }
        }
    }
}
