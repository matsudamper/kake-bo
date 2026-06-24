package net.matsudamper.money.frontend.android.feature.notificationusage

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageAccessGateway
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageAccessGateway.NotificationAccessState

internal class NotificationUsageAccessGatewayAndroidImpl(
    private val context: Context,
) : NotificationUsageAccessGateway {
    override fun accessStateFlow(): Flow<NotificationAccessState> {
        return callbackFlow {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    trySend(currentState())
                }
            }
            trySend(currentState())
            context.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor("enabled_notification_listeners"),
                false,
                observer,
            )
            awaitClose {
                context.contentResolver.unregisterContentObserver(observer)
            }
        }.distinctUntilChanged()
    }

    override fun openAccessSettings() {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun currentState(): NotificationAccessState {
        return if (NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)) {
            NotificationAccessState.Granted
        } else {
            NotificationAccessState.NotGranted
        }
    }
}
