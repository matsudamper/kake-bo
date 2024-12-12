package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object PovoUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.Povo.displayName
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

        val date = forwardedInfo?.date ?: date
        val subject = forwardedInfo?.subject ?: subject

        val price = "から(.+?)円のお支払いが完了しました".toRegex()
            .find(plain)
            ?.groups?.get(1)
            ?.value
            ?: return listOf()
        val month = """(\d+月)""".toRegex()
            .find(subject)
            ?.groups?.get(1)
            ?.value
            ?: return listOf()
        return listOf(
            MoneyUsage(
                title = "povo $month",
                description = subject,
                dateTime = date,
                price = ParseUtil.getInt(price),
                service = MoneyUsageServiceType.Povo,
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "info@povo.jp"
    }
}
