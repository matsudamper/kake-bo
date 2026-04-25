package net.matsudamper.money.frontend.android.feature.notificationusage

import android.app.Notification

internal object NotificationTextExtractor {
    fun extract(notification: Notification): String {
        val extras = notification.extras ?: return ""
        val values = buildList {
            add(extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty())
            add(extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty())
            add(extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty())
            add(extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty())
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.forEach { line -> add(line?.toString().orEmpty()) }
        }
        return values.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(separator = "\n")
    }
}
