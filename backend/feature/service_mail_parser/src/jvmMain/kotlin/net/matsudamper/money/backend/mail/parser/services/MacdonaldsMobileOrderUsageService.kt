package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil
import org.jsoup.Jsoup

internal object MacdonaldsMobileOrderUsageService : MoneyUsageServices {
    override val displayName: String = "マクドナルド"

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
            yield(canHandledWithPlain(plain))
            yield(canHandledWithHtml(html))
        }
        if (canHandle.any { it }.not()) return listOf()

        val parsed = if (plain.isNotBlank()) {
            parsePlain(plain)
        } else {
            parseHtml(html)
        }

        return listOf(
            MoneyUsage(
                title = displayName,
                dateTime = forwardedInfo?.date ?: date,
                price = parsed.price,
                service = MoneyUsageServiceType.Macdonalds,
                description = parsed.description.orEmpty(),
            ),
        )
    }

    private fun parsePlain(plain: String): ParsedOrder {
        val lines = plain.split("\r\n")
            .flatMap { it.split("\n") }

        val description = run {
            val first = lines.indexOf("数量 品目 価格").takeIf { it >= 0 } ?: return@run null
            val end = lines.indexOf("上記金額を正に領収いたしました。")
                .takeIf { it >= 0 }
                ?.minus(1) ?: return@run null

            lines.subList(first, end)
                .joinToString("\n")
        }

        val price = run price@{
            val result = "^ご請求金額(.+?)$".toRegex(RegexOption.MULTILINE)
                .find(plain)
                ?.groupValues?.getOrNull(1)
                ?: return@price null

            result.mapNotNull { it.toString().toIntOrNull() }
                .joinToString("")
                .toIntOrNull()
        }

        return ParsedOrder(
            price = price,
            description = description,
        )
    }

    private fun parseHtml(html: String): ParsedOrder {
        val document = Jsoup.parse(html)

        val price = document.getElementsByTag("td")
            .firstOrNull { it.ownText().startsWith("ご請求金額") }
            ?.nextElementSibling()
            ?.text()
            ?.let { ParseUtil.getInt(it) }

        val description = document.getElementsByTag("table")
            .lastOrNull { table -> table.getElementsByTag("th").any { it.ownText() == "品目" } }
            ?.getElementsByTag("tr")
            ?.map { tr ->
                tr.children()
                    .map { it.text().trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(" ")
            }
            ?.filter { it.isNotEmpty() }
            ?.joinToString("\n")

        return ParsedOrder(
            price = price,
            description = description,
        )
    }

    private fun canHandledWithPlain(plain: String): Boolean {
        return plain.contains("この度は、マクドナルドモバイルオーダーを")
    }

    private fun canHandledWithHtml(html: String): Boolean {
        return html.contains("この度は、マクドナルドモバイルオーダーを")
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "noreply@nsp.mdj.jp"
    }

    private data class ParsedOrder(
        val price: Int?,
        val description: String?,
    )
}
