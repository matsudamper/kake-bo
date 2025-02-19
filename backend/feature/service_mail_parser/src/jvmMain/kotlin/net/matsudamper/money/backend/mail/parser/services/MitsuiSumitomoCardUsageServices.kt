package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object MitsuiSumitomoCardUsageServices : MoneyUsageServices {
    override val displayName: String = "三井住友カード"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardOriginal = ParseUtil.parseForwarded(plain)
        val canHandle = sequence {
            yield(canHandledWithFrom(forwardOriginal?.from ?: from))
        }
        if (canHandle.any { it.not() }) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)

        val dateIndex = lines.indexOfFirst { it.contains("ご利用日時") }
            .takeIf { it >= 0 } ?: return listOf()

        val parsedDate = run {
            val dateLine = lines[dateIndex]
            val dateString = dateLine.dropWhile { it != '：' }.drop(1).trim()
            LocalDateTime.from(dateFormatter.parse(dateString))
        }

        val parsedName: String?
        val price: Int
        run {
            val matchResult = """^(.+?)([\d,]+円)""".toRegex().matchEntire(lines[dateIndex + 1])
            parsedName = matchResult?.groupValues?.get(1)
            price = matchResult?.groupValues?.get(2)?.let { ParseUtil.getInt(it) } ?: 0
        }
        return listOf(
            MoneyUsage(
                title = run {
                    if (subject.contains(DEFAULT_SUBJECT)) {
                        parsedName ?: DEFAULT_SUBJECT
                    } else {
                        subject
                    }
                },
                price = price,
                description = "",
                service = MoneyUsageServiceType.CreditCard,
                dateTime = parsedDate ?: forwardOriginal?.date ?: date,
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "statement@vpass.ne.jp"
    }

    private val dateFormatter = DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
        .appendLiteral('/')
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendLiteral('/')
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .appendLiteral(' ')
        .appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE)
        .optionalStart()
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 1, 2, SignStyle.NOT_NEGATIVE)
        .optionalEnd()
        .toFormatter()

    private const val DEFAULT_SUBJECT = "ご利用のお知らせ【三井住友カード】"
}
