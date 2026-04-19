package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageDraft
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageFilterDefinition
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord

internal class GoogleWalletNotificationUsageParser : NotificationUsageParser {
    override val filterDefinition: NotificationUsageFilterDefinition = NotificationUsageFilterDefinition(
        id = "com.google.android.apps.walletnfcrel",
        title = "Google Wallet",
        description = "Google Walletの利用通知を解析します。利用金額・利用店舗を抽出します。",
    )

    override fun parse(record: NotificationUsageRecord): NotificationUsageDraft? {
        if (record.packageName != "com.google.android.apps.walletnfcrel") return null

        val lines = record.text.lines()
        val title = lines.firstOrNull().orEmpty()
        val secondLine = lines.getOrNull(1).orEmpty()

        return NotificationUsageDraft(
            title = title,
            description = record.text,
            amount = parseAmount(secondLine),
            dateTime = Instant.fromEpochMilliseconds(record.postedAtEpochMillis).toLocalDateTime(TimeZone.currentSystemDefault()),
        )
    }

    private fun parseAmount(text: String): Int? {
        val match = Regex("""￥([0-9,]+)""").find(text) ?: return null
        return match.groupValues[1].replace(",", "").toIntOrNull()
    }
}
