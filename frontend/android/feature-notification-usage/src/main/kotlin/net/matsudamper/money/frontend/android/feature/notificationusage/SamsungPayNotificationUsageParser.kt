package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageDraft
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageFilterDefinition
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord

internal class SamsungPayNotificationUsageParser : NotificationUsageParser {
    override val filterDefinition: NotificationUsageFilterDefinition = NotificationUsageFilterDefinition(
        id = "com.samsung.android.spay",
        title = "Samsung Pay",
        description = "Samsung Payの利用通知を解析します。利用金額・利用店舗を抽出します。",
    )

    override fun parse(record: NotificationUsageRecord): NotificationUsageDraft? {
        if (record.packageName != "com.samsung.android.spay") return null

        val lines = record.text.lines()
        val secondLine = lines.getOrNull(1) ?: return null
        val amount = parseAmount(secondLine) ?: return null
        val title = parseTitle(secondLine)

        return NotificationUsageDraft(
            title = title,
            description = record.text,
            amount = amount,
            dateTime = Instant.fromEpochMilliseconds(record.postedAtEpochMillis)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
        )
    }

    private fun parseAmount(text: String): Int? {
        val match = Regex("""￥([0-9,]+)""").find(text) ?: return null
        return match.groupValues[1].replace(",", "").toIntOrNull()
    }

    private fun parseTitle(text: String): String {
        return text.replace(Regex("""￥[0-9,]+"""), "").trim()
    }
}
