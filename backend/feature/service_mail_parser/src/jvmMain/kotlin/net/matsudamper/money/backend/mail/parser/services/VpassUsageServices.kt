package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object VpassUsageServices : MoneyUsageServices {
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

        val cardTypeIndex = lines.indexOfFirst { it.startsWith("ご利用カード") }
            .takeIf { it >= 0 } ?: return listOf()
        val cardType = lines[cardTypeIndex].substringAfter("ご利用カード：").trim()

        val dateIndex = lines.indexOfFirst { it.startsWith("◇利用日") }
            .takeIf { it >= 0 } ?: return listOf()
        val parsedDate = run {
            val dateLine = lines[dateIndex]
            val dateString = dateLine.substringAfter("◇利用日：").trim()
            runCatching {
                LocalDateTime.from(dateFormatter.parse(dateString))
            }.getOrNull()
        }

        val storeIndex = lines.indexOfFirst { it.startsWith("◇利用先") }
            .takeIf { it >= 0 } ?: return listOf()
        val storeName = lines[storeIndex].substringAfter("◇利用先：").trim()

        val priceIndex = lines.indexOfFirst { it.startsWith("◇利用金額") }
            .takeIf { it >= 0 } ?: return listOf()
        val price = run {
            val priceLine = lines[priceIndex]
            val priceString = priceLine.substringAfter("◇利用金額：").trim()
            ParseUtil.getInt(priceString)
        }

        return listOf(
            MoneyUsage(
                title = storeName,
                price = price ?: 0,
                description = cardType,
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
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .toFormatter()
}
