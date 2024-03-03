package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil
import org.jsoup.Jsoup

public object SteamUsageService : MoneyUsageServices {
    override val displayName: String = "Steam"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardedOriginalInfo = ParseUtil.parseForwarded(plain)

        val canHandle =
            sequence {
                yield(canHandledWithFrom(forwardedOriginalInfo?.from ?: from))
                yield(canHandledWithSubject(forwardedOriginalInfo?.subject ?: subject))
                yield(canHandledWithPlain(plain))
            }
        if (canHandle.any { it }.not()) return listOf()

        val htmlResults =
            parseHtml(
                html = html,
                date = forwardedOriginalInfo?.date ?: date,
            )

        return if (htmlResults.isNotEmpty()) {
            htmlResults
        } else {
            listOfNotNull(
                parseOldPlain(
                    plain = plain,
                    date = forwardedOriginalInfo?.date ?: date,
                ),
            )
        }
    }

    private fun parseHtml(
        html: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val document = Jsoup.parse(html)
        val parsedDate =
            document.getElementsByTag("tr").asSequence().filter { tr ->
                tr.getElementsContainingText("発行日").isNotEmpty() &&
                    tr.getElementsByTag("td").count { it.hasText() } == 2
            }.map {
                LocalDateTime.from(dateFormat.parse(it.lastElementChild()?.text()))
            }.firstOrNull()
        return document.getElementsByTag("table").asSequence()
            .filter {
                it.getElementsByTag("strong").size == 2 &&
                    it.getElementsByTag("img").isNotEmpty() &&
                    it.getElementsContainingText("合計").isNotEmpty()
            }.map {
                it.getElementsByTag("strong")
                    .map { strong -> strong.text() }
            }.distinct().map { strongList ->
                MoneyUsage(
                    title = strongList.getOrNull(0) ?: displayName,
                    price = strongList.getOrNull(1)?.let { ParseUtil.getInt(it) } ?: 0,
                    description = "",
                    service = MoneyUsageServiceType.Steam,
                    dateTime = parsedDate ?: date,
                )
            }.toList()
    }

    private fun parseOldPlain(
        plain: String,
        date: LocalDateTime,
    ): MoneyUsage? {
        val title =
            run {
                val first = plain.indexOf("ポイントショップへ移動").takeIf { it >= 0 } ?: return@run null
                val end = plain.indexOf(startIndex = first, string = "小計").takeIf { it >= 0 } ?: return@run null

                """^\*(.+)\*$""".toRegex(RegexOption.MULTILINE).find(plain.substring(first, end))
                    ?.groupValues?.getOrNull(1)
            } ?: return null

        val price =
            run {
                val regex = """^この取引の合計:(.+?)$""".toRegex(RegexOption.MULTILINE)
                regex.find(plain)?.groupValues?.getOrNull(1)
                    ?.map { it.toString().toIntOrNull() }
                    ?.filterNotNull()
                    ?.joinToString("")
                    ?.toIntOrNull()
            }

        val parsedDate =
            run {
                val regex = """^発行日(.+?)$""".toRegex(RegexOption.MULTILINE)
                val line = regex.find(plain)?.groupValues?.getOrNull(1) ?: return@run null

                val result =
                    """(\d+).+?(\d+).+?(\d+).+?(\d+).+?(\d+)""".toRegex().find(line)
                        ?: return@run null

                val year = result.groupValues.getOrNull(1)?.toIntOrNull() ?: return@run null
                val month = result.groupValues.getOrNull(2)?.toIntOrNull() ?: return@run null
                val day = result.groupValues.getOrNull(3)?.toIntOrNull() ?: return@run null
                val hour = result.groupValues.getOrNull(4)?.toIntOrNull() ?: return@run null
                val minute = result.groupValues.getOrNull(5)?.toIntOrNull() ?: return@run null

                LocalDateTime.of(
                    LocalDate.of(year, month, day),
                    LocalTime.of(hour, minute),
                )
            }

        return MoneyUsage(
            title = title,
            price = price,
            description = "",
            service = MoneyUsageServiceType.Steam,
            dateTime = parsedDate ?: date,
        )
    }

    private fun canHandledWithPlain(plain: String): Boolean {
        return plain.contains("Steam でのお取引、ありがとうございました。")
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject == "Steam でのご購入、ありがとうございます！"
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "noreply@steampowered.com"
    }

    // 2024年1月1日 10時41分 JST
    private val dateFormat =
        DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendLiteral('年')
            .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral('月')
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral('日')
            .appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral('時')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral('分')
            .appendLiteral(" JST")
            .toFormatter()
}
