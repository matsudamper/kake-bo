package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil
import org.jsoup.Jsoup

internal object UberEatsUsageService : MoneyUsageServices {
    override val displayName: String = "UberEats"

    override fun parse(subject: String, from: String, html: String, plain: String, date: LocalDateTime): List<MoneyUsage> {
        val canHandle = sequence {
            yield(canHandledWithFrom(from))
            yield(canHandledWithSubject(subject))
            yield(canHandledWithHtml(html))
        }
        if (canHandle.any { it }.not()) return listOf()

        val htmlDocument = Jsoup.parse(html)
        val price = run price@{
            val regex = "^合計.*?￥(.+?\\d)$".toRegex(RegexOption.MULTILINE)
            val result = htmlDocument.allElements.asSequence()
                .mapNotNull { regex.find(it.text()) }
                .firstOrNull()
                ?.groupValues?.getOrNull(1) ?: return@price null
            ParseUtil.getInt(result)
        }

        val title = run price@{
            val regex = "^(.+?)の領収書をお受け取りください。$".toRegex(RegexOption.MULTILINE)
            val value = htmlDocument.allElements.asSequence()
                .mapNotNull { regex.find(it.text()) }
                .firstOrNull()
                ?.groupValues?.getOrNull(1)
                ?: return@price ""
            value
        }

        return listOf(
            MoneyUsage(
                title = title,
                price = price,
                description = "",
                service = MoneyUsageServiceType.UberEats,
                dateTime = date,
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "noreply@uber.com"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.startsWith("Uber Eats のご注文")
    }

    private fun canHandledWithHtml(value: String): Boolean {
        return value.contains("Uber Eats Japan合同会社")
    }
}
