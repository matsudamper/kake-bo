package platform

import kotlinx.browser.window
import net.matsudamper.money.ui.root.platform.UrlOpener

internal class UrlOpenerImpl : UrlOpener {
    override fun open(url: String) {
        window.open(url)
    }
}
