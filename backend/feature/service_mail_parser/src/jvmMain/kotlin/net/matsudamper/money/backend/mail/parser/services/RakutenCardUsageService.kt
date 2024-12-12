package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object RakutenCardUsageService : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.RakutenCard.displayName

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
        }
        if (canHandle.any { it }.not()) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)
        val priceTitleIndex = lines.indexOfFirst { it.startsWith("*ご利用金額*") }
        val priceLine = lines.getOrNull(priceTitleIndex + 1) ?: return listOf()

        val regex = """^(\d{4})/(\d{2})/(\d{2}) (.+?) (.+?) 円$""".toRegex()
            .find(priceLine)

        val year = regex?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return listOf()
        val month = regex.groupValues.getOrNull(2)?.toIntOrNull() ?: return listOf()
        val day = regex.groupValues.getOrNull(3)?.toIntOrNull() ?: return listOf()
        val price = regex.groupValues.getOrNull(5)
            ?.let { ParseUtil.getInt(it) } ?: return listOf()

        return listOf(
            MoneyUsage(
                title = subject,
                price = price,
                description = "",
                service = MoneyUsageServiceType.RakutenCard,
                dateTime = LocalDateTime.of(
                    LocalDate.of(year, month, day),
                    LocalTime.of(0, 0),
                ),
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "info@mail.rakuten-card.co.jp"
    }
}
