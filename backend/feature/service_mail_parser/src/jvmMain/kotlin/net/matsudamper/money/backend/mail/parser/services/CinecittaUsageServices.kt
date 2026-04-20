package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object CinecittaUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.Cinecitta.displayName

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)
        if (
            canHandle(
                from = forwardedInfo?.from ?: from,
                subject = forwardedInfo?.subject ?: subject,
                plain = plain,
            ).not()
        ) {
            return listOf()
        }

        val lines = ParseUtil.splitByNewLine(plain)
            .map { it.trim() }
        val reservationLines = getSectionLines(
            lines = lines,
            startLabel = "[予約]",
            endLabel = "[合計（税込）]",
        )
        val orderDateText = getNextContentLine(
            lines = lines,
            label = "[注文日時]",
        )
        val price = getNextContentLine(
            lines = lines,
            label = "[合計（税込）]",
        )?.let { ParseUtil.getInt(it) }
        val title = parseTitle(reservationLines)
        val usageDateTime = parseDateTime(reservationLines)
        val orderDateTime = orderDateText?.let { parseDateTime(listOf(it)) }

        return listOf(
            MoneyUsage(
                title = title ?: displayName,
                price = price,
                description = createDescription(
                    orderDateText = orderDateText,
                    reservationLines = reservationLines,
                ),
                service = MoneyUsageServiceType.Cinecitta,
                dateTime = usageDateTime ?: orderDateTime ?: forwardedInfo?.date ?: date,
            ),
        )
    }

    private fun canHandle(
        from: String,
        subject: String,
        plain: String,
    ): Boolean {
        val canHandleHeader = from == "ticket@ml.smart-theater.com" &&
            subject.contains("予約完了のお知らせ")
        val canHandleBody = plain.contains("チネチッタのチケット") ||
            plain.contains("cinecitta-production")
        return canHandleHeader && canHandleBody
    }

    private fun createDescription(
        orderDateText: String?,
        reservationLines: List<String>,
    ): String {
        return buildList {
            if (orderDateText != null) {
                add("注文日時: $orderDateText")
            }
            addAll(reservationLines)
        }.joinToString("\n")
    }

    private fun getNextContentLine(
        lines: List<String>,
        label: String,
    ): String? {
        val labelIndex = lines.indexOf(label).takeIf { it >= 0 } ?: return null
        return lines.drop(labelIndex + 1)
            .firstOrNull { it.isNotBlank() }
    }

    private fun getSectionLines(
        lines: List<String>,
        startLabel: String,
        endLabel: String,
    ): List<String> {
        val startIndex = lines.indexOf(startLabel).takeIf { it >= 0 } ?: return listOf()
        val endIndex = lines.withIndex()
            .firstOrNull { (index, line) -> index > startIndex && line == endLabel }
            ?.index ?: lines.size

        return lines.subList(startIndex + 1, endIndex)
            .filter { it.isNotBlank() }
    }

    private fun parseDateTime(reservationLines: List<String>): LocalDateTime? {
        val line = reservationLines.firstOrNull {
            """\d{4}/\d{2}/\d{2}""".toRegex().containsMatchIn(it)
        } ?: return null
        val result = """(\d{4})/(\d{2})/(\d{2}).*?(\d{1,2}):(\d{2})""".toRegex()
            .find(line) ?: return null

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

    private fun parseTitle(reservationLines: List<String>): String? {
        val titleLine = reservationLines.firstOrNull() ?: return null
        return titleLine
            .replace("""\s*／\s*\+?[\d,]+円.*$""".toRegex(), "")
            .trim()
            .takeIf { it.isNotEmpty() }
    }
}
