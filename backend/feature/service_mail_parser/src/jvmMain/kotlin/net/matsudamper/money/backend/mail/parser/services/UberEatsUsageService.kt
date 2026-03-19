package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

internal object UberEatsUsageService : MoneyUsageServices {
    override val displayName: String = "UberEats"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val canHandle = sequence {
            yield(canHandledWithFrom(from))
            yield(canHandledWithSubject(subject))
            yield(canHandledWithHtml(html))
        }
        if (canHandle.any { it }.not()) return listOf()

        val htmlDocument = Jsoup.parse(html)
        val price = newParser.parsePrice(htmlDocument) ?: oldParser.parsePrice(htmlDocument)
        val title = (newParser.parseTitle(htmlDocument) ?: oldParser.parseTitle(htmlDocument)).orEmpty()

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

    private val newParser = object {
        fun parsePrice(htmlDocument: Document): Int? {
            val amountText = htmlDocument.select("[data-testid=total_fare_amount], .total-fare-amount").firstOrNull()?.text()
                ?: htmlDocument.allElements.asSequence()
                    .filter { it.text().startsWith("合計") }
                    .firstOrNull { it.text().contains("￥") }
                    ?.text()
            val match = "￥\\s?([\\d,]+)".toRegex().find(amountText ?: return null)
            return ParseUtil.getInt(match?.groupValues?.getOrNull(1) ?: return null)
        }

        fun parseTitle(htmlDocument: Document): String? {
            val regex = "(.+?)の領収書をお受け取りください。".toRegex()
            return htmlDocument.getElementsMatchingOwnText(regex.pattern)
                .firstNotNullOfOrNull {
                    regex.find(it.text())?.groupValues?.getOrNull(1)
                }?.trim()
        }
    }

    private val oldParser = object {
        fun parsePrice(htmlDocument: Document): Int? {
            val regex = "^合計.*?￥(.+?\\d)$".toRegex(RegexOption.MULTILINE)
            val result = htmlDocument.allElements
                .firstNotNullOfOrNull { regex.find(it.text()) }
                ?.groupValues?.getOrNull(1) ?: return null
            return ParseUtil.getInt(result)
        }

        fun parseTitle(htmlDocument: Document): String? {
            val regex = "(.+?)の領収書をお受け取りください。".toRegex()
            return htmlDocument.allElements.asSequence()
                .mapNotNull { regex.find(it.text())?.groupValues?.getOrNull(1) }
                .firstOrNull()?.trim()
        }
    }
}
