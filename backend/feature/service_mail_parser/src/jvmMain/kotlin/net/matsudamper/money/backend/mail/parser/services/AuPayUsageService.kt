package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object AuPayUsageService : MoneyUsageServices {
    override val displayName: String = "au PAY"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)
        val canHandle = sequence {
            yield(canHandledWithFrom(forwardedInfo?.from ?: from))
            yield(canHandledWithSubject(forwardedInfo?.subject ?: subject))
        }
        if (canHandle.all { it }.not()) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)
        val price = run price@{
            val startIndex = lines.indexOf("■支払い金額")
                .takeIf { it >= 0 }
                ?: return@price 0

            val priceString = lines.getOrNull(startIndex + 1) ?: return@price 0
            ParseUtil.getInt(priceString) ?: 0
        }
        val title = run title@{
            val startIndex = lines.indexOf("■利用店舗")
                .takeIf { it >= 0 }
                ?: return@title null

            lines.getOrNull(startIndex + 1)?.trim()
        }
        val parsedDate = run date@{
            val startIndex = lines.indexOf("■利用日時")
                .takeIf { it >= 0 }
                ?: return@date null

            // 2024/07/03 11:23
            val dateString = lines.getOrNull(startIndex + 1) ?: return@date null

            val temp = runCatching {
                DateTimeFormatterBuilder()
                    .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                    .appendLiteral('/')
                    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                    .appendLiteral('/')
                    .appendValue(ChronoField.DAY_OF_MONTH, 2)
                    .appendLiteral(' ')
                    .append(DateTimeFormatter.ISO_LOCAL_TIME)
                    .toFormatter()
                    .parse(dateString.trim())
            }.onFailure {
                it.printStackTrace()
            }.getOrNull()
            LocalDateTime.from(temp)
        }

        return listOf(
            MoneyUsage(
                title = title.orEmpty(),
                dateTime = parsedDate ?: forwardedInfo?.date ?: date,
                price = price,
                service = MoneyUsageServiceType.AuPay,
                description = title.orEmpty(),
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "auto@connect.auone.jp"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.startsWith("【au PAY】ご利用のお知らせ")
    }
}
