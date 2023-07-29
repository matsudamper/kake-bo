package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

internal object PayPalUsageService : MoneyUsageServices {
    override val displayName: String = "PayPal"

    override fun parse(subject: String, from: String, html: String, plain: String, date: LocalDateTime): List<MoneyUsage> {
        val canHandle = sequence {
            yield(canHandledWithFrom(from))
            yield(canHandledWithHtml(plain))
        }
        if (canHandle.any { it }.not()) return listOf()

        val priceRawText: String?
        val price = run price@{
            val texts = Jsoup.parse(html).select("strong").toList()
                .asSequence()
                .filter { it.text() == "合計" }
                .mapNotNull { it.getParentElement("tr") }
                .flatMap { it.select("span") }
                .map { it.text() }

            priceRawText = texts
                .filterNot { it.contains("合計") }
                .joinToString(", ")
                .takeIf { it.isNotBlank() }

            texts.mapNotNull { text ->
                text.mapNotNull { it.toString().toIntOrNull() }
                    .joinToString("")
                    .toIntOrNull()
            }.firstOrNull()
        }

        val title = run title@{
            val title = Jsoup.parse(html).select("title").text()
            "^(.+?)様への支払いの領収書".toRegex().find(title)?.groupValues?.getOrNull(1)
                ?: return@title null
        }

        val price2: Int?
        run {
            val result = "(?<=>)(.+?)への(.+?)のお支払いが実行されました<".toRegex()
                .find(html)
                ?.groupValues

            price2 = result?.getOrNull(2)
                ?.mapNotNull { it.toString().toIntOrNull() }
                ?.joinToString("")
                ?.toIntOrNull()
        }

        return listOf(
            MoneyUsage(
                title = title ?: displayName,
                price = price ?: price2,
                description = buildString {
                    if (priceRawText != null) {
                        appendLine(priceRawText)
                    }
                },
                service = MoneyUsageServiceType.PayPal,
                dateTime = date,
            ),
        )
    }

    private infix fun Element.getParentElement(tagName: String): Element? {
        val parent = parent() ?: return null
        return if (parent.tagName() == tagName) {
            parent
        } else {
            parent.getParentElement(tagName)
        }
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "service-jp@paypal.com"
    }

    private fun canHandledWithHtml(html: String): Boolean {
        return html.contains("PayPalは、売り手の代行として買い手から支払いを受け取ります。売り手は、PayPalが買い手からの支払いを受諾すると、買い手は支払い金額についてそれ以上の責任を負わないことに同意するものとします。")
    }
}
