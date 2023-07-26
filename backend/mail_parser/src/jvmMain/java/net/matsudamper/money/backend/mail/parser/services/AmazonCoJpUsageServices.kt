package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import org.jsoup.Jsoup

internal object AmazonCoJpUsageServices : MoneyUsageServices {
    override val displayName: String = "Amazon"

    override fun parse(subject: String, from: String, html: String, plain: String, date: LocalDateTime): MoneyUsage? {
        val canHandle = sequence {
            yield(canHandledWithFrom(from))
            yield(canHandledWithSubject(subject))
            yield(canHandledWithPlain(plain))
        }
        if (canHandle.any { it }.not()) return null

        val isGiftCard = plain.contains("ギフトカードの送信先")

        val price = sequence {
            yield(
                run price@{
                    val total = plain
                        .split("\r\n")
                        .flatMap { it.split("\n") }
                        .firstOrNull { it.contains("注文合計：") }
                        ?: return@price null

                    "注文合計：".let { targetText ->
                        total.substring(total.indexOf(targetText) + targetText.length)
                    }
                        .toList()
                        .mapNotNull { it.toString().toIntOrNull() }
                        .joinToString("")
                        .toIntOrNull()
                },
            )
            yield(
                run price@{
                    val startIndex = plain.indexOf("注文合計:").takeIf { it >= 0 } ?: return@price null

                    "￥(.+?)$".toRegex(RegexOption.MULTILINE)
                        .find(startIndex = startIndex, input = plain)
                        ?.groupValues?.getOrNull(1)
                        ?.mapNotNull { it.toString().toIntOrNull() }
                        ?.joinToString("")
                        ?.toIntOrNull()
                },
            )
        }.filterNotNull().firstOrNull()

        val url = Jsoup.parse(html).getElementsByClass("buttonComponentLink").attr("href")
            .takeIf { it.isNotBlank() }

        val mailDateTime = run {
            val result = """注文日： (\d{4})/(\d{2})/(\d{2})""".toRegex().find(plain) ?: return@run null
            val year = result.groupValues.getOrNull(1)?.toIntOrNull() ?: return@run null
            val month = result.groupValues.getOrNull(2)?.toIntOrNull() ?: return@run null
            val day = result.groupValues.getOrNull(3)?.toIntOrNull() ?: return@run null

            LocalDate.of(year, month, day)
        }

        return MoneyUsage(
            title = if (isGiftCard) "Amazonギフトカード" else "Amazon購入",
            price = price,
            description = url.orEmpty(),
            service = MoneyUsageServiceType.Amazon,
            dateTime = run {
                if (mailDateTime != null) {
                    LocalDateTime.of(mailDateTime, LocalTime.MIN)
                } else {
                    date
                }
            },
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "auto-confirm@amazon.co.jp"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.startsWith("Amazon.co.jpでのご注文")
    }

    private fun canHandledWithPlain(plain: String): Boolean {
        return sequence {
            yield(plain.startsWith("Amazon.co.jp注文の確認"))
            yield(plain.startsWith("Amazon.co.jp ご注文の確認"))
        }.any { it }
    }
}
