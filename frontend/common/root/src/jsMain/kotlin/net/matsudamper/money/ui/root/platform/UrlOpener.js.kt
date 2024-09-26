package net.matsudamper.money.ui.root.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

@Composable
actual fun rememberUrlOpener(): UrlOpener {
    return remember { UrlOpenerImpl() }
}

private class UrlOpenerImpl : UrlOpener {
    override fun open(url: String) {
        window.open(url)
    }
}