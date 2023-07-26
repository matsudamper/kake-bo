package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices

internal object UberEatsUsageService : MoneyUsageServices {
    override val displayName: String = "UberEats"

    override fun parse(subject: String, from: String, html: String, plain: String, date: LocalDateTime): List<MoneyUsage> {
        val canHandle = sequence {
            yield(canHandledWithFrom(from))
            yield(canHandledWithSubject(subject))
            yield(canHandledWithPlain(plain))
        }
        if (canHandle.any { it }.not()) return listOf()

        val price = run price@{
            "合計 ￥(.+?)$".toRegex(RegexOption.MULTILINE)
                .find(plain)
                ?.groupValues?.getOrNull(1)
                ?.mapNotNull { it.toString().toIntOrNull() }
                ?.joinToString("")
                ?.toIntOrNull()
        }

        val title = run price@{
            "^(.+?)の領収書をお受け取りください。$".toRegex(RegexOption.MULTILINE)
                .find(plain)
                ?.groupValues?.getOrNull(1)
                ?.trimStart()
                ?.trimEnd()
        }

        return listOf(
            MoneyUsage(
                title = "[$displayName]$title",
                price = price,
                description = "",
                service = MoneyUsageServiceType.UberEats,
                dateTime = date,
            )
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "noreply@uber.com"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.startsWith("Uber Eats のご注文")
    }

    private fun canHandledWithPlain(plain: String): Boolean {
        return plain.contains("Uber Eats Japan合同会社")
    }
}