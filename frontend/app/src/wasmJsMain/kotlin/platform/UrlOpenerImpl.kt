package platform

import kotlinx.browser.window
import net.matsudamper.money.ui.root.platform.UrlOpener

internal class UrlOpenerImpl : UrlOpener {
    override fun open(url: String) {
        // javascript: 等を開くと同一オリジンでスクリプトが実行されるため http(s) 以外は開かない
        val isHttpUrl = url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)
        if (!isHttpUrl) return
        window.open(url)
    }
}
