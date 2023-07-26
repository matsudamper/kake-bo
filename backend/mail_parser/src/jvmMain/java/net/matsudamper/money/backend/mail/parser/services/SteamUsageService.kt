package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
public object SteamUsageService : MoneyUsageServices {
    override val displayName: String = "Steam"

    override fun parse(subject: String, from: String, html: String, plain: String, date: LocalDateTime): List<MoneyUsage> {
        val canHandle = sequence {
            yield(canHandledWithFrom(from))
            yield(canHandledWithSubject(subject))
            yield(canHandledWithPlain(plain))
        }
        if (canHandle.any { it }.not()) return listOf()

        val title = run {
            val first = plain.indexOf("ポイントショップへ移動").takeIf { it >= 0 } ?: return@run null
            val end = plain.indexOf(startIndex = first, string = "小計").takeIf { it >= 0 } ?: return@run null

            """^\*(.+)\*$""".toRegex(RegexOption.MULTILINE).find(plain.substring(first, end))
                ?.groupValues?.getOrNull(1)
        }

        val price = run {
            val regex = """^この取引の合計:(.+?)$""".toRegex(RegexOption.MULTILINE)
            regex.find(plain)?.groupValues?.getOrNull(1)
                ?.map { it.toString().toIntOrNull() }
                ?.filterNotNull()
                ?.joinToString("")
                ?.toIntOrNull()
        }

        val parsedDate = run {
            val regex = """^発行日(.+?)$""".toRegex(RegexOption.MULTILINE)
            val line = regex.find(plain)?.groupValues?.getOrNull(1) ?: return@run null

            val result = """(\d+).+?(\d+).+?(\d+).+?(\d+).+?(\d+)""".toRegex().find(line)
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

        return listOf(
            MoneyUsage(
                title = title ?: displayName,
                price = price,
                description = "",
                service = MoneyUsageServiceType.Steam,
                dateTime = parsedDate ?: date,
            )
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
}
