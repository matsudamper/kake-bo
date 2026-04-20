package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object TixplusTradeUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.TixplusTrade.displayName

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)
        val targetFrom = forwardedInfo?.from ?: from
        val targetSubject = forwardedInfo?.subject ?: subject
        if (canHandle(from = targetFrom, subject = targetSubject).not()) {
            return listOf()
        }

        val lines = ParseUtil.splitByNewLine(plain)
        val artist = getFieldValue(lines, "アーティスト")
        val venue = getFieldValue(lines, "会場")
        val description = buildDescription(
            receiptNumber = getReceiptNumber(plain),
            performanceDate = getFieldValue(lines, "公演日"),
            venue = venue,
            seatType = getFieldValue(lines, "座席種別"),
            ticketCount = getFieldValue(lines, "枚数"),
        )

        return listOf(
            MoneyUsage(
                title = artist?.plus(" ")?.plus(venue) ?: displayName,
                price = getTotalPrice(lines),
                description = description,
                service = MoneyUsageServiceType.TixplusTrade,
                dateTime = forwardedInfo?.date ?: date,
            ),
        )
    }

    private fun buildDescription(
        receiptNumber: String?,
        performanceDate: String?,
        venue: String?,
        seatType: String?,
        ticketCount: String?,
    ): String {
        return buildList {
            if (receiptNumber != null) {
                add("受付番号: $receiptNumber")
            }
            if (performanceDate != null) {
                add("公演日: $performanceDate")
            }
            if (venue != null) {
                add("会場: $venue")
            }
            if (seatType != null) {
                add("座席種別: $seatType")
            }
            if (ticketCount != null) {
                add("枚数: $ticketCount")
            }
        }.joinToString("\n")
    }

    private fun getFieldValue(
        lines: List<String>,
        label: String,
    ): String? {
        val fullWidthPrefix = "$label："
        val halfWidthPrefix = "$label:"

        for (line in lines) {
            if (line.startsWith(fullWidthPrefix)) {
                return line.removePrefix(fullWidthPrefix).trim()
            }
            if (line.startsWith(halfWidthPrefix)) {
                return line.removePrefix(halfWidthPrefix).trim()
            }
        }

        return null
    }

    private fun getReceiptNumber(plain: String): String? {
        return "【受付番号：(.+?)】".toRegex()
            .find(plain)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
    }

    private fun getTotalPrice(lines: List<String>): Int? {
        val paymentTotalStartIndex = lines.indexOf("【支払総額】")
            .takeIf { it >= 0 }
        val targetLines = if (paymentTotalStartIndex != null) {
            lines.drop(paymentTotalStartIndex + 1)
        } else {
            lines
        }

        val totalLine = targetLines.firstOrNull { line ->
            line.trim().startsWith("=") && line.contains("円")
        } ?: return null

        return ParseUtil.getInt(totalLine)
    }

    private fun canHandle(
        from: String,
        subject: String,
    ): Boolean {
        return from == "trade@mail.plusmember.jp" &&
            subject.contains("【チケプラトレード】購入完了のお知らせ")
    }
}
