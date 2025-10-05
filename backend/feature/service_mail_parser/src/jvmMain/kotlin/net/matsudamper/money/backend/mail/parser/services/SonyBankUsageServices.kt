package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object SonyBankUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.SonyBank.displayName

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
            yield(canHandledWithSubject(subject))
        }
        if (canHandle.all { it }.not()) return listOf()

        return parsePlain(plain)
    }

    private fun parsePlain(plain: String): List<MoneyUsage> {
        val lines = ParseUtil.splitByNewLine(plain)

        val dateLineIndex = lines.indexOfFirst { it.startsWith("カード利用日：") }
            .takeIf { it != -1 } ?: return listOf()

        val dateLine = lines[dateLineIndex].removePrefix("カード利用日：")
        val dateRegex = """(\d{4})年(\d{1,2})月(\d{1,2})日""".toRegex()
        val dateMatch = dateRegex.find(dateLine) ?: return listOf()

        val year = dateMatch.groupValues[1].toIntOrNull() ?: return listOf()
        val month = dateMatch.groupValues[2].toIntOrNull() ?: return listOf()
        val day = dateMatch.groupValues[3].toIntOrNull() ?: return listOf()

        val amountLineIndex = lines.indexOfFirst { it.startsWith("ご利用金額：") }
            .takeIf { it != -1 } ?: return listOf()

        val amountLine = lines[amountLineIndex].removePrefix("ご利用金額：")
        val price = ParseUtil.getInt(amountLine.replace("円", "")) ?: return listOf()

        val merchantLineIndex = lines.indexOfFirst { it.startsWith("ご利用加盟店：") }
            .takeIf { it != -1 } ?: return listOf()

        val merchantName = lines[merchantLineIndex].removePrefix("ご利用加盟店：")

        return listOf(
            MoneyUsage(
                title = merchantName,
                price = price,
                description = "",
                service = MoneyUsageServiceType.SonyBank,
                dateTime = LocalDateTime.of(
                    LocalDate.of(year, month, day),
                    LocalTime.of(0, 0),
                ),
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "banking@sonybank.jp"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.contains("Sony Bank WALLET ご利用のお知らせ")
    }
}
