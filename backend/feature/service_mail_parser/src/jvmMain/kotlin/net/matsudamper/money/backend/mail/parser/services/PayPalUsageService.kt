package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

internal object PayPalUsageService : MoneyUsageServices {
    override val displayName: String = "PayPal"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val canHandle =
            sequence {
                yield(canHandledWithFrom(from))
                yield(canHandledWithHtml(plain))
            }
        if (canHandle.any { it }.not()) return listOf()

        val document = Jsoup.parse(html)
        val priceRawText: String?
        val price =
            run price@{
                val texts =
                    document.select("strong").toList()
                        .asSequence()
                        .filter { it.text() == "合計" }
                        .mapNotNull { it.getParentElement("tr") }
                        .flatMap { it.select("span") }
                        .map { it.text() }

                priceRawText =
                    texts
                        .filterNot { it.contains("合計") }
                        .joinToString(", ")
                        .takeIf { it.isNotBlank() }

                texts.mapNotNull { text ->
                    text.mapNotNull { it.toString().toIntOrNull() }
                        .joinToString("")
                        .toIntOrNull()
                }.firstOrNull()
            }

        val title =
            run title@{
                val title = document.select("title").text()
                "^(.+?)様への支払いの領収書".toRegex().find(title)?.groupValues?.getOrNull(1)
                    ?: return@title null
            }

        val price2: Int?
        run {
            val result =
                "(?<=>)(.+?)への(.+?)のお支払いが実行されました<".toRegex()
                    .find(html)
                    ?.groupValues

            price2 =
                result?.getOrNull(2)
                    ?.mapNotNull { it.toString().toIntOrNull() }
                    ?.joinToString("")
                    ?.toIntOrNull()
        }

        val parsedDate =
            run date@{
                val tmp =
                    document.select(".ppsans").asSequence()
                        .filter { it.select("strong").any { strong -> strong.text() == "取引日" } }
                        .mapNotNull { it.select("span").getOrNull(1) }
                        .map { "2023年11月10日 3:45:19 JST" }
                        .onEach { println("paypal date") }
                        .mapNotNull {
                            runCatching { dateFormat.parse(it) }
                                .onFailure { it.printStackTrace() }
                                .getOrNull()
                        }
                        .firstOrNull()
                        ?: return@date null
                LocalDateTime.from(tmp)
            }

        return listOf(
            MoneyUsage(
                title = title ?: displayName,
                price = price ?: price2,
                description =
                buildString {
                    if (priceRawText != null) {
                        appendLine(priceRawText)
                    }
                    if (parsedDate == null) {
                        appendLine("エラー情報")
                        appendLine("日付のパースに失敗しました")
                    }
                }.trim(),
                service = MoneyUsageServiceType.PayPal,
                dateTime = parsedDate ?: date,
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

    // 2023年11月10日 3:45:19 JST
    private val dateFormat =
        DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendLiteral('年')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('月')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral('日')
            .appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 1, 2, SignStyle.NOT_NEGATIVE)
            .optionalEnd()
            .appendLiteral(" JST")
            .toFormatter()
}
