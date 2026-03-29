package net.matsudamper.money.frontend.common.base.platform

public interface ApplicationNotificationManager {
    public fun notify(message: String)

    public suspend fun requestNotificationPermission()
}
