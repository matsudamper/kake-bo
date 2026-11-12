package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object SmtCinemaUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.SmtCinema.displayName

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
            yield(canHandledWithSubject(forwardedInfo?.subject ?: subject))
        }
        if (canHandle.all { it }.not()) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)
            .map { it.trim() }
        val theaterLines = getSectionLines(lines = lines, label = "劇場")
        val movieTitle = getSectionLines(lines = lines, label = "作品名").firstOrNull()
        val scheduleLines = getSectionLines(lines = lines, label = "日時")
        val seatLines = getSectionLines(lines = lines, label = "座席番号")
        val ticketLines = getSectionLines(lines = lines, label = "チケット")

        return listOf(
            MoneyUsage(
                title = movieTitle ?: displayName,
                price = parseTotalPrice(ticketLines),
                description = buildList {
                    addAll(theaterLines)
                    addAll(scheduleLines)
                    seatLines.forEach { add("座席: $it") }
                    addAll(ticketLines)
                }.joinToString("\n"),
                service = MoneyUsageServiceType.SmtCinema,
                dateTime = parseDateTime(scheduleLines) ?: forwardedInfo?.date ?: date,
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "ticket@smt-cinema.com"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.contains("チケット購入の確認")
    }

    private fun getSectionLines(
        lines: List<String>,
        label: String,
    ): List<String> {
        val labelIndex = lines.indexOf("▼$label").takeIf { it >= 0 } ?: return listOf()
        return lines.drop(labelIndex + 1)
            .takeWhile { it.startsWith("▼").not() && it.startsWith("★").not() }
            .filter { it.isNotBlank() }
    }

    private fun parseTotalPrice(ticketLines: List<String>): Int? {
        val prices = ticketLines.mapNotNull { line ->
            val result = """([\d,]+)円/(\d+)枚""".toRegex().find(line) ?: return@mapNotNull null
            val unitPrice = ParseUtil.getInt(result.groupValues[1]) ?: return@mapNotNull null
            val count = result.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            unitPrice * count
        }
        return prices.takeIf { it.isNotEmpty() }?.sum()
    }

    private fun parseDateTime(scheduleLines: List<String>): LocalDateTime? {
        val dateResult = scheduleLines.firstNotNullOfOrNull {
            """(\d{4})/(\d{1,2})/(\d{1,2})""".toRegex().find(it)
        } ?: return null
        val localDate = LocalDate.of(
            dateResult.groupValues[1].toIntOrNull() ?: return null,
            dateResult.groupValues[2].toIntOrNull() ?: return null,
            dateResult.groupValues[3].toIntOrNull() ?: return null,
        )
        val timeResult = scheduleLines.firstNotNullOfOrNull {
            """(\d{1,2}):(\d{2})""".toRegex().find(it)
        } ?: return localDate.atStartOfDay()
        return LocalDateTime.of(
            localDate,
            LocalTime.of(
                timeResult.groupValues[1].toIntOrNull() ?: return null,
                timeResult.groupValues[2].toIntOrNull() ?: return null,
            ),
        )
    }
}
