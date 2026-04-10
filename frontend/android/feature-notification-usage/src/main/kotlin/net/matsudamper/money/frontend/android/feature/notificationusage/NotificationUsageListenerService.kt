package net.matsudamper.money.frontend.android.feature.notificationusage

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecordInput
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRepository
import org.koin.core.context.GlobalContext

public class NotificationUsageListenerService : NotificationListenerService() {
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeNotifications?.forEach { notification ->
            persist(notification)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return
        persist(sbn)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun persist(sbn: StatusBarNotification) {
        val repository = runCatching { GlobalContext.get().get<NotificationUsageRepository>() }
            .getOrNull()
            ?: return
        val autoAddProcessor = runCatching { GlobalContext.get().get<NotificationUsageAutoAddProcessor>() }
            .getOrNull()
        val text = NotificationTextExtractor.extract(sbn.notification)
        val input = NotificationUsageRecordInput(
            notificationKey = sbn.key,
            packageName = sbn.packageName,
            text = text,
            postedAtEpochMillis = sbn.postTime,
            receivedAtEpochMillis = System.currentTimeMillis(),
        )
        scope.launch {
            repository.upsertNotification(input)
            autoAddProcessor?.process(input.notificationKey)
        }
    }
}
