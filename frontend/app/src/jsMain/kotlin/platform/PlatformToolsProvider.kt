package platform

import net.matsudamper.money.ui.root.platform.ApplicationNotificationManager
import net.matsudamper.money.ui.root.platform.ClipboardManager
import net.matsudamper.money.ui.root.platform.PlatformTools
import net.matsudamper.money.ui.root.platform.UrlOpener

internal class PlatformToolsProvider : PlatformTools {
    override val urlOpener: UrlOpener = UrlOpenerImpl()
    override val clipboardManager: ClipboardManager = ClipboardManagerImpl()
    override val applicationNotificationManager: ApplicationNotificationManager = ApplicationNotificationManagerImpl()
}
