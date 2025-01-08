package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object AmazonCoJpMonthlyUsageServices : MoneyUsageServices {
    override val displayName: String = "Amazon定期お得便"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)
        if (
            canHandle(
                from = forwardedInfo?.from ?: from,
                fromText = forwardedInfo?.fromPersonal,
            ).not()
        ) return listOf()
        val lines = ParseUtil.splitByNewLine(plain)
        val totalIndex = lines.indexOfFirst { it.startsWith("注文合計(税込)") }
        val totalPrice = ParseUtil.getInt(lines[totalIndex])
        return listOf(
            MoneyUsage(
                title = displayName,
                price = totalPrice,
                description = "",
                service = MoneyUsageServiceType.Amazon,
                dateTime = forwardedInfo?.date ?: date,
            ),
        )
    }

    private fun canHandle(
        from: String,
        fromText: String?,
    ): Boolean {
        return from == "no-reply@amazon.co.jp" &&
                (fromText?.contains("定期おトク便") != false)
    }
}
