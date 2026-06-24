package net.matsudamper.money.ui.root.platform

public interface ApplicationNotificationManager {
    public fun notify(message: String)

    public suspend fun requestNotificationPermission()
}
