package net.matsudamper.money.platform

import androidx.activity.ComponentActivity
import net.matsudamper.money.ui.root.platform.ClipboardManager
import net.matsudamper.money.ui.root.platform.PlatformTools
import net.matsudamper.money.ui.root.platform.UrlOpener

internal class PlatFormToolsImpl(
    componentActivity: ComponentActivity,
) : PlatformTools {
    override val urlOpener: UrlOpener = UrlOpenerImpl(componentActivity)
    override val clipboardManager: ClipboardManager = ClipboardManagerImpl(componentActivity)
}
