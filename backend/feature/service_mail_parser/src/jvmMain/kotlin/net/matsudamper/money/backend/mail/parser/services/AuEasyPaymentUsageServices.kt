package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import java.util.Locale
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object AuEasyPaymentUsageServices : MoneyUsageServices {
    override val displayName: String = "auかんたん決済"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)
        val canHandle =
            sequence {
                yield(canHandledWithFrom(forwardedInfo?.from ?: from))
                yield(canHandledWithSubject(forwardedInfo?.subject ?: subject))
            }
        if (canHandle.all { it }.not()) return listOf()

        val plainLines = ParseUtil.splitByNewLine(plain)

        val price =
            run {
                val priceText = getUseDetail(plainLines, "ご利用金額") ?: return@run null
                ParseUtil.getInt(priceText)
            }
        val parsedDate =
            run {
                val dateText = getUseDetail(plainLines, "ご利用日")

                val tmp =
                    DateTimeFormatterBuilder()
                        .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                        .appendLiteral('/')
                        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                        .appendLiteral('/')
                        .appendValue(ChronoField.DAY_OF_MONTH, 2)
                        .appendLiteral(' ')
                        .append(DateTimeFormatter.ISO_LOCAL_TIME)
                        .toFormatter()
                        .withLocale(Locale.JAPANESE)
                        .parse(dateText)

                LocalDateTime.from(tmp)
            }
        val shopName = getUseDetail(plainLines, "加盟店名")
        val serviceName = getUseDetail(plainLines, "サービス名")
        val description = getUseDetail(plainLines, "摘要")
        return listOf(
            MoneyUsage(
                title = serviceName ?: displayName,
                price = price,
                description =
                    buildString {
                        appendLine(displayName)
                        if (shopName != null) {
                            appendLine("加盟店名: $shopName")
                        }
                        if (description != null) {
                            appendLine("摘要: $description")
                        }
                    }.trim(),
                service = MoneyUsageServiceType.AuEasyPayment,
                dateTime = parsedDate ?: forwardedInfo?.date ?: date,
            ),
        )
    }

    private fun getUseDetail(
        lines: List<String>,
        title: String,
    ): String? {
        val index =
            lines.indexOfFirst { it.startsWith(title) }
                .takeIf { it >= 0 } ?: return null

        val targetLine = lines.getOrNull(index) ?: return null
        return "：(.+?)$".toRegex()
            .find(targetLine)
            ?.groupValues
            ?.getOrNull(1)
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "auto@connect.auone.jp"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject == "auかんたん決済 ご利用内容のお知らせ"
    }
}
