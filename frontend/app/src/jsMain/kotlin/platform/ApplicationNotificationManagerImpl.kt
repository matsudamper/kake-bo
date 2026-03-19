package platform

import kotlinx.browser.window
import net.matsudamper.money.frontend.common.base.platform.ApplicationNotificationManager

internal class ApplicationNotificationManagerImpl : ApplicationNotificationManager {
    override fun notify(message: String) {
        window.alert(message)
    }
}
