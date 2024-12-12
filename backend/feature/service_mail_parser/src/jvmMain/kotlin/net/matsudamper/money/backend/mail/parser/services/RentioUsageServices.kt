package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object RentioUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.Rentio.displayName

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)
        val canHandle = sequence {
            yield(canHandled(from = from, subject = subject))
        }
        if (canHandle.any { it }.not()) return listOf()
        val price = ParseUtil.splitByNewLine(plain)
            .firstOrNull { it.startsWith("ご請求金額") }
            ?.let { ParseUtil.getInt(it) }
            ?: return listOf()

        return listOf(
            MoneyUsage(
                title = MoneyUsageServiceType.Rentio.displayName,
                price = price,
                description = "",
                service = MoneyUsageServiceType.Rentio,
                dateTime = forwardedInfo?.date ?: date,
            ),
        )
    }

    private fun canHandled(
        from: String,
        subject: String,
    ): Boolean {
        return from == "support@rentio.jp" && subject.contains("レンタル料金が決済されました")
    }
}
