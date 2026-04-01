package net.matsudamper.money.frontend.common.base.platform

public interface PlatformTools {
    public val urlOpener: UrlOpener
    public val clipboardManager: ClipboardManager
    public val applicationNotificationManager: ApplicationNotificationManager
    public val backPressDispatcher: BackPressDispatcher
    public val imagePicker: ImagePicker
}
