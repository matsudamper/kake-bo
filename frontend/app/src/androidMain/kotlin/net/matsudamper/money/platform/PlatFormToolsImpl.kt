package net.matsudamper.money.platform

import androidx.activity.ComponentActivity
import net.matsudamper.money.frontend.common.base.platform.ApplicationNotificationManager
import net.matsudamper.money.frontend.common.base.platform.BackPressDispatcher
import net.matsudamper.money.frontend.common.base.platform.ClipboardManager
import net.matsudamper.money.frontend.common.base.platform.PlatformTools
import net.matsudamper.money.frontend.common.base.platform.UrlOpener

internal class PlatFormToolsImpl(
    componentActivity: ComponentActivity,
    requestNotificationPermission: (callback: (Boolean) -> Unit) -> Unit,
) : PlatformTools {
    override val urlOpener: UrlOpener = UrlOpenerImpl(componentActivity)
    override val clipboardManager: ClipboardManager = ClipboardManagerImpl(componentActivity)
    override val applicationNotificationManager: ApplicationNotificationManager = ApplicationNotificationManagerImpl(componentActivity, requestNotificationPermission)
    override val backPressDispatcher: BackPressDispatcher = BackPressDispatcherImpl(componentActivity)
    override val imagePicker = ImagePickerImpl(componentActivity)
}
