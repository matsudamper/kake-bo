package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Immutable
public interface KakeboScaffoldListener {
    public fun onClickTitle()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun KakeboScaffold(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            if (topBar != null) {
                topBar()
            }
        },
        snackbarHost = snackbarHost,
        bottomBar = bottomBar,
    ) {
        content(it)
    }
}
