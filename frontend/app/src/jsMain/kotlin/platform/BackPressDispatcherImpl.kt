package platform

import kotlinx.browser.window
import net.matsudamper.money.frontend.common.base.platform.BackPressDispatcher

internal class BackPressDispatcherImpl : BackPressDispatcher {
    override fun onBackPressed() {
        window.history.back()
    }
}
