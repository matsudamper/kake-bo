package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object ViewCardUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.ViewCard.displayName

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
            .map { it.removePrefix("・").trim() }

        val parsedDate = lines.firstOrNull { it.startsWith("利用日") }
            ?.let { parseValue(it) }
            ?.let { parseUsageDate(it) }
        val parsedPrice = lines.firstOrNull { it.startsWith("利用金額") }
            ?.let { parseValue(it) }
            ?.let { ParseUtil.getInt(it) }
        val store = lines.firstOrNull { it.startsWith("利用加盟店") }
            ?.let { parseValue(it) }
        val card = lines.firstOrNull { it.startsWith("ご利用カード") }
            ?.let { parseValue(it) }

        val title = store ?: card ?: return listOf()

        return listOf(
            MoneyUsage(
                title = title,
                price = parsedPrice,
                description = card.orEmpty(),
                service = MoneyUsageServiceType.ViewCard,
                dateTime = parsedDate ?: actualDate,
            ),
        )
    }

    private fun parseValue(line: String): String? {
        return line.substringAfter("：", "")
            .trim()
            .takeIf { it.isNotEmpty() }
    }

    private fun parseUsageDate(dateStr: String): LocalDateTime? {
        val parts = dateStr.split("/")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val month = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val day = parts.getOrNull(2)?.toIntOrNull() ?: return null
        return LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.of(0, 0))
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "viewcard@mail.viewsnet.jp"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.contains("ビューカードご利用情報のお知らせ")
    }
}
