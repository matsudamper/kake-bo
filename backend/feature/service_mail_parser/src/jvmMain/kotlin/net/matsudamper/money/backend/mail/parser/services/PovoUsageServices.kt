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
        val actualFrom = forwardedInfo?.from ?: from
        val actualSubject = forwardedInfo?.subject ?: subject
        val actualDate = forwardedInfo?.date ?: date

        val canHandle = sequence {
            yield(canHandledWithFrom(actualFrom))
            yield(canHandledWithSubject(actualSubject))
        }
        if (canHandle.all { it }.not()) return listOf()

        val price = "から(.+?)円のお支払いが完了しました".toRegex()
            .find(ParseUtil.removeHtmlTag(html))
            ?.groups?.get(1)
            ?.value
            ?: return listOf()
        val month = """(\d+月ご利用分)""".toRegex()
            .find(actualSubject)
            ?.groups?.get(1)
            ?.value
            ?: return listOf()
        return listOf(
            MoneyUsage(
                title = "povo $month",
                description = actualSubject,
                dateTime = actualDate,
                price = ParseUtil.getInt(price),
                service = MoneyUsageServiceType.Povo,
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "info@povo.jp"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.contains("povo") &&
            subject.contains("月ご利用分のご請求のお支払いが完了しました")
    }
}
