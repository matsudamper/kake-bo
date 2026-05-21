package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageDraft
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageFilterDefinition
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord

internal class PayPayNotificationUsageParser : NotificationUsageParser {
    override val filterDefinition: NotificationUsageFilterDefinition = NotificationUsageFilterDefinition(
        id = "jp.ne.paypay.android.app",
        title = "PayPay",
        description = "PayPayの取引完了通知を解析します。利用金額・店舗名を抽出します。",
    )

    override fun parse(record: NotificationUsageRecord): NotificationUsageDraft? {
        if (record.packageName != "jp.ne.paypay.android.app") return null

        val lines = record.text.lines()
        if (lines.getOrNull(1) != "取引が完了しました。") return null

        val amount = parseAmount(record.text) ?: return null
        val title = parseStoreName(record.text) ?: return null

        return NotificationUsageDraft(
            title = title,
            description = record.text,
            amount = amount,
            dateTime = Instant.fromEpochMilliseconds(record.postedAtEpochMillis)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
        )
    }

    private fun parseAmount(text: String): Int? {
        // "金額：XXX円" のような利用金額を抽出する
        val match = Regex("""金額：([0-9,]+)円""").find(text) ?: return null
        return match.groupValues[1].replace(",", "").toIntOrNull()
    }

    private fun parseStoreName(text: String): String? {
        // "店舗名：XXX" のような店舗名を抽出する
        val match = Regex("""店舗名：(.+)""").find(text) ?: return null
        return match.groupValues[1].trim()
    }
}
