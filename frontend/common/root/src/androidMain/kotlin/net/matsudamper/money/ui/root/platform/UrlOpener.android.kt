package net.matsudamper.money.ui.root.platform

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

@Composable
actual fun rememberUrlOpener(): UrlOpener {
    val context = LocalContext.current
    return remember(context) { UrlOpenerImpl(context) }
}

private class UrlOpenerImpl(
    private val context: Context,
) : UrlOpener {
    override fun open(url: String) {
        // TODO use custom tab
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}
