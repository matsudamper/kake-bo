package platform

import kotlinx.browser.window
import net.matsudamper.money.ui.root.platform.ApplicationNotificationManager

internal class ApplicationNotificationManagerImpl : ApplicationNotificationManager {
    override fun notify(message: String) {
        window.alert(message)
    }
}
