package platform

import kotlinx.browser.window
import net.matsudamper.money.ui.root.platform.BackPressDispatcher

internal class BackPressDispatcherImpl : BackPressDispatcher {
    override fun onBackPressed() {
        window.history.back()
    }
}
