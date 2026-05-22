package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object RakutenCardUsageService : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.RakutenCard.displayName

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
        }
        if (canHandle.any { it }.not()) return listOf()

        return when {
            plain.contains("カード利用お知らせメール(確定版)") -> parseConfirmedInfo(plain)
            plain.contains("■利用日:") -> parseMultipleUsages(plain, date)
            else -> parseFastInfo(plain, subject)
        }
    }

    private fun parseConfirmedInfo(plain: String): List<MoneyUsage> {
        val lines = ParseUtil.splitByNewLine(plain)

        val regex = """^(\d{4})/(\d{2})/(\d{2}) (.+?) (.+?) 円$""".toRegex()

        return buildList {
            for (line in lines) {
                val result = regex.find(line)
                val year = result?.groupValues?.getOrNull(1)?.toIntOrNull() ?: continue
                val month = result.groupValues.getOrNull(2)?.toIntOrNull() ?: continue
                val day = result.groupValues.getOrNull(3)?.toIntOrNull() ?: continue
                val title = result.groupValues.getOrNull(4) ?: continue
                val price = result.groupValues.getOrNull(5)
                    ?.let { ParseUtil.getInt(it) } ?: continue

                add(
                    MoneyUsage(
                        title = title,
                        price = price,
                        description = "",
                        service = MoneyUsageServiceType.RakutenCard,
                        dateTime = LocalDateTime.of(
                            LocalDate.of(year, month, day),
                            LocalTime.of(0, 0),
                        ),
                    ),
                )
            }
        }
    }

    private fun parseMultipleUsages(plain: String, fallbackDate: LocalDateTime): List<MoneyUsage> {
        val lines = ParseUtil.splitByNewLine(plain)
        val result = mutableListOf<MoneyUsage>()

        var currentDate: LocalDateTime? = null
        var currentTitle: String? = null
        var currentPrice: Int? = null

        fun flush() {
            val title = currentTitle ?: return
            result.add(
                MoneyUsage(
                    title = title,
                    price = currentPrice,
                    description = "",
                    service = MoneyUsageServiceType.RakutenCard,
                    dateTime = currentDate ?: fallbackDate,
                ),
            )
        }

        for (line in lines) {
            when {
                line.startsWith("■利用日:") -> {
                    flush()
                    currentDate = parseUsageDate(line.removePrefix("■利用日:").trim()) ?: fallbackDate
                    currentTitle = null
                    currentPrice = null
                }
                line.startsWith("■利用先:") -> {
                    currentTitle = line.removePrefix("■利用先:").trim()
                }
                line.startsWith("■利用金額:") -> {
                    currentPrice = ParseUtil.getInt(line.removePrefix("■利用金額:").trim())
                }
            }
        }
        flush()

        return result
    }

    private fun parseUsageDate(dateStr: String): LocalDateTime? {
        val parts = dateStr.split("/")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val month = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val day = parts.getOrNull(2)?.toIntOrNull() ?: return null
        return LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.of(0, 0))
    }

    private fun parseFastInfo(plain: String, subject: String): List<MoneyUsage> {
        val lines = ParseUtil.splitByNewLine(plain)
        val priceTitleIndex = lines.indexOfFirst { it.startsWith("*ご利用金額*") }
        val priceLine = lines.getOrNull(priceTitleIndex + 1) ?: return listOf()

        val result = """^(\d{4})/(\d{2})/(\d{2}) (.+?) (.+?) 円$""".toRegex()
            .find(priceLine)

        val year = result?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return listOf()
        val month = result.groupValues.getOrNull(2)?.toIntOrNull() ?: return listOf()
        val day = result.groupValues.getOrNull(3)?.toIntOrNull() ?: return listOf()
        val price = result.groupValues.getOrNull(5)
            ?.let { ParseUtil.getInt(it) } ?: return listOf()

        return listOf(
            MoneyUsage(
                title = subject,
                price = price,
                description = "",
                service = MoneyUsageServiceType.RakutenCard,
                dateTime = LocalDateTime.of(
                    LocalDate.of(year, month, day),
                    LocalTime.of(0, 0),
                ),
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "info@mail.rakuten-card.co.jp"
    }
}
