package net.matsudamper.money.ui.root.platform

import androidx.compose.runtime.Composable

public interface UrlOpener {
    fun open(url: String)
}

@Composable
public expect fun rememberUrlOpener(): UrlOpener