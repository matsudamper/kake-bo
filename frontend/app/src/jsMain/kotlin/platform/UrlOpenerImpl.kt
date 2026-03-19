package platform

import kotlinx.browser.window
import net.matsudamper.money.frontend.common.base.platform.UrlOpener

internal class UrlOpenerImpl : UrlOpener {
    override fun open(url: String) {
        window.open(url)
    }
}
