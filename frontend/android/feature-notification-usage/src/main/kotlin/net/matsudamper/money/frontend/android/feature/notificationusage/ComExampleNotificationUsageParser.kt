package net.matsudamper.money.frontend.android.feature.notificationusage

import net.matsudamper.money.frontend.common.base.notification.NotificationUsageDraft
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageFilterDefinition
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord

internal class ComExampleNotificationUsageParser : NotificationUsageParser {
    override val filterDefinition: NotificationUsageFilterDefinition = NotificationUsageFilterDefinition(
        id = "com.example",
        title = "com.example サンプル",
        description = "packageName が com.example に一致した通知を対象にし、通知全文を description に入れます。",
    )

    override fun parse(record: NotificationUsageRecord): NotificationUsageDraft? {
        if (record.packageName != "com.example") return null

        return NotificationUsageDraft(
            description = record.text,
        )
    }
}
