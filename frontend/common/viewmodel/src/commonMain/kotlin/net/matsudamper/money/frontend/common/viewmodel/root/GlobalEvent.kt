package net.matsudamper.money.frontend.common.viewmodel.root

public interface GlobalEvent {
    public fun showSnackBar(message: String)
    public fun showNativeNotification(message: String)
}