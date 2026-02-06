package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
public class ScrollToTopHandler {
    private var handler: (() -> Boolean)? = null

    public fun register(handler: () -> Boolean) {
        this.handler = handler
    }

    public fun unregister() {
        this.handler = null
    }

    /**
     * 子がスクロールをTOPに移動した場合はtrueを返す。
     * 既にTOPにいる場合はfalseを返し、親がハンドリングする。
     */
    public fun requestScrollToTop(): Boolean = handler?.invoke() ?: false
}

public val LocalScrollToTopHandler: androidx.compose.runtime.ProvidableCompositionLocal<ScrollToTopHandler> =
    staticCompositionLocalOf { ScrollToTopHandler() }
