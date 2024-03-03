package net.matsudamper.money.frontend.common.viewmodel.root

import androidx.compose.runtime.Immutable

@Immutable
public interface GlobalEvent {
    public fun showSnackBar(message: String)

    public fun showNativeNotification(message: String)
}
