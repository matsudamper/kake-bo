package net.matsudamper.money.frontend.android.feature.notificationusage

import android.app.Notification
import android.service.notification.StatusBarNotification

internal object NotificationMetadataExtractor {
    fun extract(sbn: StatusBarNotification): String {
        val notification = sbn.notification
        val flags = notification.flags
        return buildString {
            appendLine("id=${sbn.id}")
            appendLine("tag=${sbn.tag}")
            appendLine("isOngoing=${sbn.isOngoing}")
            appendLine("isClearable=${sbn.isClearable}")
            appendLine("flags=$flags")
            appendLine("flags_hex=0x${flags.toString(16)}")
            appendLine("flag_ongoing=${(flags and Notification.FLAG_ONGOING_EVENT) != 0}")
            appendLine("flag_foregroundService=${(flags and Notification.FLAG_FOREGROUND_SERVICE) != 0}")
            appendLine("flag_autoCancel=${(flags and Notification.FLAG_AUTO_CANCEL) != 0}")
            appendLine("flag_noClear=${(flags and Notification.FLAG_NO_CLEAR) != 0}")
            appendLine("flag_groupSummary=${(flags and Notification.FLAG_GROUP_SUMMARY) != 0}")
            appendLine("flag_localOnly=${(flags and Notification.FLAG_LOCAL_ONLY) != 0}")
            appendLine("category=${notification.category}")
            appendLine("priority=${notification.priority}")
            appendLine("visibility=${notification.visibility}")
            appendLine("group=${notification.group}")
            appendLine("sortKey=${notification.sortKey}")
            append("channelId=${notification.channelId}")
        }
    }
}
