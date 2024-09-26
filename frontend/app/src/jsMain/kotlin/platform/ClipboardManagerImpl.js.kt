package platform

import kotlinx.browser.window
import net.matsudamper.money.ui.root.platform.ClipboardManager

internal class ClipboardManagerImpl : ClipboardManager {
    override fun copy(text: String) {
        window.navigator.clipboard.writeText(text)
    }
}
