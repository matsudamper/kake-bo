package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices

internal object ESekiReserveUsegeService : MoneyUsageServices {
    override val displayName: String = "e席リザーブ"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val canHandle = sequence {
            yield(canHandledWithFrom(from))
            yield(canHandledWithSubject(subject))
            yield(canHandledWithPlain(plain))
        }
        if (canHandle.any { it }.not()) return listOf()

        val lines = plain
            .split("\r\n")
            .flatMap { it.split("\n") }

        val purchaseDateText = run {
            val index = lines.indexOfFirst { it.contains("＜購入日時＞") }.takeIf { it >= 0 }
                ?: return@run null
            lines.getOrNull(index + 1)?.trim()
        }

        val ticketInfoLines = run {
            val startIndex = lines.indexOfFirst { it.contains("＜チケット情報＞") }.takeIf { it >= 0 }
                ?: return@run listOf()
            val endIndex = lines.withIndex()
                .firstOrNull { (index, line) -> index > startIndex && line.contains("＜") }
                ?.index
                ?: lines.size
            lines.subList(startIndex + 1, endIndex).filter { it.isNotBlank() }
        }

        val title = ticketInfoLines.firstOrNull()?.trim()

        val parsedDate = run date@{
            val dateLine = ticketInfoLines.firstOrNull { line ->
                """\d{4}/\d{2}/\d{2}""".toRegex().containsMatchIn(line)
            } ?: return@date null

            val result = """(\d{4})/(\d{2})/(\d{2}).*?(\d{2}):(\d{2})""".toRegex()
                .find(dateLine) ?: return@date null

            val year = result.groupValues.getOrNull(1)?.toIntOrNull() ?: return@date null
            val month = result.groupValues.getOrNull(2)?.toIntOrNull() ?: return@date null
            val day = result.groupValues.getOrNull(3)?.toIntOrNull() ?: return@date null
            val hour = result.groupValues.getOrNull(4)?.toIntOrNull() ?: return@date null
            val minute = result.groupValues.getOrNull(5)?.toIntOrNull() ?: return@date null

            LocalDateTime.of(
                LocalDate.of(year, month, day),
                LocalTime.of(hour, minute),
            )
        }

        val individualPrices = ticketInfoLines.mapNotNull { line ->
            """￥(\d+)""".toRegex().find(line)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }

        val totalPrice = run {
            val index = lines.indexOfFirst { it.contains("＜合計（税込）＞") }.takeIf { it >= 0 }
                ?: return@run null
            """￥(\d+)""".toRegex()
                .find(lines.getOrNull(index + 1).orEmpty())
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
        }

        val allSamePrice = individualPrices.isNotEmpty() && individualPrices.distinct().size == 1

        val price = if (allSamePrice) {
            individualPrices.first()
        } else {
            totalPrice
        }

        val description = buildString {
            if (purchaseDateText != null) {
                append(purchaseDateText)
            }
            if (!allSamePrice && ticketInfoLines.isNotEmpty()) {
                if (isNotEmpty()) append("\n")
                append(ticketInfoLines.joinToString("\n"))
            }
        }

        return listOf(
            MoneyUsage(
                title = title ?: displayName,
                price = price,
                description = description,
                service = MoneyUsageServiceType.ESekiReserve,
                dateTime = parsedDate ?: date,
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "e-reserve@aeonent.jp"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.startsWith("e席リザーブ")
    }

    private fun canHandledWithPlain(plain: String): Boolean {
        return plain.contains("e席リザーブ")
    }
}
