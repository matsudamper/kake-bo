package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

/**
 * 例:
 * From: info@highwaybus.com
 * Subject: [highwaybus.com]WEB決済完了のお知らせ
 * 乗車日：XXXX/XX/XX(X)
 * 乗車バス停：XXXXXXX（XX:XX発車予定）
 * 降車バス停：XXXXXXXX
 * 決済金額： \X,XXX
 */
internal object HighwayBusUsageServices : MoneyUsageServices {
    override val displayName: String = "ハイウェイバスドットコム"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)
        val canHandle = sequence {
            yield(canHandled(from = from, subject = subject))
            yield(
                run {
                    if (forwardedInfo != null) {
                        val forwardedFrom = forwardedInfo.from ?: return@run false
                        val forwardedSubject = forwardedInfo.subject ?: return@run false
                        canHandled(from = forwardedFrom, subject = forwardedSubject)
                    } else {
                        false
                    }
                },
            )
        }
        if (canHandle.any { it }.not()) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)

        val boardingStop = getBoardingStop(lines) ?: return listOf()
        val alightingStop = getAlightingStop(lines) ?: return listOf()
        val boardingDateTime = getBoardingDateTime(lines) ?: return listOf()
        val price = getPrice(lines) ?: return listOf()

        return listOf(
            MoneyUsage(
                title = "$boardingStop → $alightingStop",
                price = price,
                description = "",
                service = MoneyUsageServiceType.HighwayBus,
                dateTime = boardingDateTime,
            ),
        )
    }

    private fun getBoardingStop(lines: List<String>): String? {
        val line = lines.firstOrNull { it.startsWith("乗車バス停：") } ?: return null
        val value = line.removePrefix("乗車バス停：")
        return value.replace("（\\d{1,2}:\\d{2}発車予定）".toRegex(), "").trim()
    }

    private fun getAlightingStop(lines: List<String>): String? {
        val line = lines.firstOrNull { it.startsWith("降車バス停：") } ?: return null
        return line.removePrefix("降車バス停：").trim()
    }

    private fun getBoardingDateTime(lines: List<String>): LocalDateTime? {
        val dateLine = lines.firstOrNull { it.startsWith("乗車日：") } ?: return null
        val dateMatch = "(\\d{4})/(\\d{2})/(\\d{2})".toRegex()
            .find(dateLine) ?: return null
        val year = dateMatch.groupValues[1].toIntOrNull() ?: return null
        val month = dateMatch.groupValues[2].toIntOrNull() ?: return null
        val day = dateMatch.groupValues[3].toIntOrNull() ?: return null

        val boardingLine = lines.firstOrNull { it.startsWith("乗車バス停：") } ?: return null
        val timeMatch = "（(\\d{1,2}):(\\d{2})".toRegex()
            .find(boardingLine) ?: return null
        val hour = timeMatch.groupValues[1].toIntOrNull() ?: return null
        val minute = timeMatch.groupValues[2].toIntOrNull() ?: return null

        return LocalDateTime.of(
            LocalDate.of(year, month, day),
            LocalTime.of(hour, minute),
        )
    }

    private fun getPrice(lines: List<String>): Int? {
        val line = lines.firstOrNull { it.startsWith("決済金額：") } ?: return null
        return ParseUtil.getInt(line)
    }

    private fun canHandled(from: String, subject: String): Boolean {
        return from == "info@highwaybus.com" && subject.contains("[highwaybus.com]WEB決済完了のお知らせ")
    }
}
