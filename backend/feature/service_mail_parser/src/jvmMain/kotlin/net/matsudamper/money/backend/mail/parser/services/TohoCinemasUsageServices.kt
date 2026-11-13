package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object TohoCinemasUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.TohoCinemas.displayName

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
        if (canHandle.any { it }.not()) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)
            .map { it.trim() }

        val movieTitle = getLabeledValue(lines = lines, label = "■作品名")
        val theater = getLabeledValue(lines = lines, label = "■映画館")
        val screen = lines.firstOrNull { it.startsWith("■スクリーン") }
            ?.removePrefix("■")
        val seat = getLabeledValue(lines = lines, label = "■座席番号")
        val showDateText = getLabeledValue(lines = lines, label = "■上映日")
        val showTimeText = getLabeledValue(lines = lines, label = "■時間")
        val price = getLabeledValue(lines = lines, label = "■合計金額")
            ?.let { ParseUtil.getInt(it) }

        return listOf(
            MoneyUsage(
                title = movieTitle ?: displayName,
                price = price,
                description = buildList {
                    if (theater != null) add(theater)
                    if (screen != null) add(screen)
                    if (seat != null) add("座席: $seat")
                    if (showDateText != null && showTimeText != null) add("$showDateText $showTimeText")
                }.joinToString("\n"),
                service = MoneyUsageServiceType.TohoCinemas,
                dateTime = parseShowDateTime(
                    dateText = showDateText,
                    timeText = showTimeText,
                ) ?: actualDate,
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "i-net.ticket@ml.tohotheater.jp"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.contains("【TOHO CINEMAS】インターネットチケット購入完了のお知らせ")
    }

    private fun getLabeledValue(
        lines: List<String>,
        label: String,
    ): String? {
        val line = lines.firstOrNull { it.startsWith(label) } ?: return null
        return line.removePrefix(label)
            .trim(' ', '　')
            .takeIf { it.isNotEmpty() }
    }

    private fun parseShowDateTime(
        dateText: String?,
        timeText: String?,
    ): LocalDateTime? {
        if (dateText == null) return null
        val dateResult = """(\d{4})/(\d{1,2})/(\d{1,2})""".toRegex().find(dateText) ?: return null
        val localDate = LocalDate.of(
            dateResult.groupValues[1].toInt(),
            dateResult.groupValues[2].toInt(),
            dateResult.groupValues[3].toInt(),
        )
        val timeResult = timeText?.let { """(\d{1,2}):(\d{2})""".toRegex().find(it) }
        val localTime = if (timeResult != null) {
            LocalTime.of(
                timeResult.groupValues[1].toInt(),
                timeResult.groupValues[2].toInt(),
            )
        } else {
            LocalTime.MIDNIGHT
        }
        return LocalDateTime.of(localDate, localTime)
    }
}
