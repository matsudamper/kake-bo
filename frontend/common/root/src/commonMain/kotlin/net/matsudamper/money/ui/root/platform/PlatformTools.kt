package net.matsudamper.money.ui.root.platform

public interface PlatformTools {
    public val urlOpener: UrlOpener
    public val clipboardManager: ClipboardManager
    public val applicationNotificationManager: ApplicationNotificationManager
    public val backPressDispatcher: BackPressDispatcher
    public val imagePicker: ImagePicker
}
