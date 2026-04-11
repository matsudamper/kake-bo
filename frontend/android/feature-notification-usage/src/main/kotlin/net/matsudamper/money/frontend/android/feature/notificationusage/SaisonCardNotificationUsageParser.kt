package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageDraft
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageFilterDefinition
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord

internal class SaisonCardNotificationUsageParser : NotificationUsageParser {
    override val filterDefinition: NotificationUsageFilterDefinition = NotificationUsageFilterDefinition(
        id = "jp.co.saisoncard.android.saisonportal",
        title = "セゾンカード",
        description = "セゾンカードのご利用通知を解析します。利用金額・利用場所・利用日時を抽出します。",
    )

    override fun parse(record: NotificationUsageRecord): NotificationUsageDraft? {
        if (record.packageName != "jp.co.saisoncard.android.saisonportal") return null

        return NotificationUsageDraft(
            title = parsePlace(record.text),
            description = record.text,
            amount = parseAmount(record.text),
            dateTime = parseDateTime(record.text),
        )
    }

    private fun parseAmount(text: String): Int? {
        // "金額：3,111円" のような利用金額を抽出する
        val match = Regex("""金額：([0-9,]+)円""").find(text) ?: return null
        return match.groupValues[1].replace(",", "").toIntOrNull()
    }

    private fun parsePlace(text: String): String? {
        // "場所：DMM.com" のような利用場所を抽出する
        val match = Regex("""場所：(.+)""").find(text) ?: return null
        return match.groupValues[1].trim()
    }

    private fun parseDateTime(text: String): LocalDateTime? {
        // "日時：2026年4月11日 12時33分" のような利用日時を抽出する
        val match = Regex("""日時：(\d+)年(\d+)月(\d+)日\s+(\d+)時(\d+)分""").find(text) ?: return null
        val (year, month, day, hour, minute) = match.destructured
        return runCatching {
            LocalDateTime(
                date = LocalDate(year.toInt(), month.toInt(), day.toInt()),
                time = LocalTime(hour.toInt(), minute.toInt()),
            )
        }.getOrNull()
    }
}
