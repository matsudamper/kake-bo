package platform

import net.matsudamper.money.frontend.common.base.platform.ApplicationNotificationManager
import net.matsudamper.money.frontend.common.base.platform.BackPressDispatcher
import net.matsudamper.money.frontend.common.base.platform.ClipboardManager
import net.matsudamper.money.frontend.common.base.platform.ImagePicker
import net.matsudamper.money.frontend.common.base.platform.PlatformTools
import net.matsudamper.money.frontend.common.base.platform.UrlOpener

internal class PlatformToolsProvider : PlatformTools {
    override val urlOpener: UrlOpener = UrlOpenerImpl()
    override val clipboardManager: ClipboardManager = ClipboardManagerImpl()
    override val applicationNotificationManager: ApplicationNotificationManager = ApplicationNotificationManagerImpl()
    override val backPressDispatcher: BackPressDispatcher = BackPressDispatcherImpl()
    override val imagePicker: ImagePicker = ImagePickerImpl()
}
