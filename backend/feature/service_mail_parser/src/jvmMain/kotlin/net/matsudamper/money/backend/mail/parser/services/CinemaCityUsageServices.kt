package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object CinemaCityUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.CinemaCity.displayName

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)
        val actualFrom = forwardedInfo?.from ?: from
        val actualSubject = forwardedInfo?.subject ?: subject
        val actualDate = forwardedInfo?.date ?: date

        val canHandle = sequence {
            yield(canHandledWithFrom(actualFrom))
            yield(canHandledWithSubject(actualSubject))
        }
        if (canHandle.all { it }.not()) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)
            .map { it.trim() }

        val screeningTimeText = getNextContentLine(lines = lines, label = "■上映時間")
        val theater = getNextContentLine(lines = lines, label = "■劇場")
        val seat = getNextContentLine(lines = lines, label = "■座席")
        val ticketCount = getNextContentLine(lines = lines, label = "■枚数")
        val price = getNextContentLine(lines = lines, label = "■合計金額")
            ?.let { ParseUtil.getInt(it) }

        return listOf(
            MoneyUsage(
                title = parseTitle(lines) ?: displayName,
                price = price,
                description = buildList {
                    if (screeningTimeText != null) add("上映時間: $screeningTimeText")
                    if (theater != null) add("劇場: $theater")
                    if (seat != null) add("座席: $seat")
                    if (ticketCount != null) add("枚数: $ticketCount")
                }.joinToString("\n"),
                service = MoneyUsageServiceType.CinemaCity,
                dateTime = screeningTimeText?.let { parseDateTime(it) } ?: actualDate,
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "ticket@cinemacity.co.jp"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.contains("チケットご予約完了のお知らせ")
    }

    private fun getNextContentLine(
        lines: List<String>,
        label: String,
    ): String? {
        val labelIndex = lines.indexOf(label).takeIf { it >= 0 } ?: return null
        return lines.drop(labelIndex + 1)
            .firstOrNull { it.isNotBlank() }
    }

    private fun parseTitle(lines: List<String>): String? {
        val screeningTimeLabelIndex = lines.indexOf("■上映時間").takeIf { it >= 0 } ?: return null
        return lines.take(screeningTimeLabelIndex)
            .lastOrNull { it.isNotBlank() }
    }

    private fun parseDateTime(text: String): LocalDateTime? {
        val result = """(\d{4})年(\d{1,2})月(\d{1,2})日.*?(\d{1,2}):(\d{2})""".toRegex()
            .find(text) ?: return null

        val year = result.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val month = result.groupValues.getOrNull(2)?.toIntOrNull() ?: return null
        val day = result.groupValues.getOrNull(3)?.toIntOrNull() ?: return null
        val hour = result.groupValues.getOrNull(4)?.toIntOrNull() ?: return null
        val minute = result.groupValues.getOrNull(5)?.toIntOrNull() ?: return null

        return LocalDateTime.of(
            LocalDate.of(year, month, day),
            LocalTime.of(hour, minute),
        )
    }
}
