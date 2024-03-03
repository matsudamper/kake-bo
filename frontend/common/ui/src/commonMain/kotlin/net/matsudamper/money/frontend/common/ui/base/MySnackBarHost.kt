package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarDefaults.actionColor
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@Composable
public fun MySnackBarHost(
    modifier: Modifier = Modifier,
    hostState: SnackbarHostState,
) {
    SnackbarHost(
        modifier = modifier,
        hostState = hostState,
    ) { snackbarData ->
        val actionLabel = snackbarData.visuals.actionLabel
        Snackbar(
            modifier = Modifier.padding(12.dp),
            action =
                if (actionLabel != null) {
                    {
                        TextButton(
                            colors = ButtonDefaults.textButtonColors(contentColor = actionColor),
                            onClick = { snackbarData.performAction() },
                            content = {
                                Text(
                                    text = actionLabel,
                                    fontFamily = rememberCustomFontFamily(),
                                )
                            },
                        )
                    }
                } else {
                    null
                },
            dismissAction =
                if (snackbarData.visuals.withDismissAction) {
                    {
                        IconButton(
                            onClick = { snackbarData.dismiss() },
                            content = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                } else {
                    null
                },
            actionOnNewLine = false,
            shape = SnackbarDefaults.shape,
            containerColor = SnackbarDefaults.color,
            contentColor = SnackbarDefaults.contentColor,
            actionContentColor = SnackbarDefaults.actionContentColor,
            dismissActionContentColor = SnackbarDefaults.dismissActionContentColor,
            content = {
                Text(
                    text = snackbarData.visuals.message,
                    fontFamily = rememberCustomFontFamily(),
                )
            },
        )
    }
}
