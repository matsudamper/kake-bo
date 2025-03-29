package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object AmazonPayUsageServices : MoneyUsageServices {
    override val displayName: String = "Amazon Pay"

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
        if (canHandle.any { it }.not()) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)

        val title = run {
            val shopPrefix = "販売事業者お問い合わせ先 "
            lines.firstOrNull { it.startsWith(shopPrefix) }
                ?.drop(shopPrefix.length)
                ?.trim()
        }

        val price = run {
            val prefix = "ご請求金額 "
            lines.firstOrNull { it.startsWith(prefix) }
                ?.drop(prefix.length)
                ?.let { ParseUtil.getInt(it) }
        }

        return listOf(
            MoneyUsage(
                title = title.orEmpty(),
                price = price,
                description = "",
                service = MoneyUsageServiceType.AmazonPay,
                dateTime = date,
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "no-reply@amazon.com"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.startsWith("Amazon Pay")
    }
}
