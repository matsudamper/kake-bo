package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageDraft
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageFilterDefinition
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord

internal class MobileSuicaNotificationUsageParser : NotificationUsageParser {
    override val filterDefinition: NotificationUsageFilterDefinition = NotificationUsageFilterDefinition(
        id = "com.felicanetworks.mfm.main",
        title = "モバイルSuica",
        description = "モバイルSuicaの利用通知を解析します。利用金額と利用日時を抽出します。",
    )

    override fun parse(record: NotificationUsageRecord): NotificationUsageDraft? {
        if (record.packageName != "com.felicanetworks.mfm.main") return null
        val line = record.text.split("\n")
        val title = line.getOrNull(0) ?: return null
        if (title != "モバイルSuica") return null

        // 支払いなし・チャージなど有効な支払い金額がない場合は対象外
        val amount = parseAmount(record.text) ?: return null

        return NotificationUsageDraft(
            title = filterDefinition.title,
            description = record.text,
            amount = amount,
            dateTime = Instant.fromEpochMilliseconds(record.postedAtEpochMillis)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
        )
    }

    private fun parseAmount(text: String): Int? {
        // "-555円" や "-1,234円" のような利用金額を抽出する
        // チャージ（プラスの値）や支払いなしの場合はマッチしない
        val match = Regex("-([0-9,]+)円").find(text) ?: return null
        return match.groupValues[1].replace(",", "").toIntOrNull()
    }
}
