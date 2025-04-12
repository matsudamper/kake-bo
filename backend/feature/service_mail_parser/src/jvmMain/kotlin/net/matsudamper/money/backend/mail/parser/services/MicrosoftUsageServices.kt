package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.collections.firstOrNull
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object MicrosoftUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.Microsoft.displayName

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)

        val canHandle = sequence {
            yield(canFromHandle(from = forwardedInfo?.from ?: from))
            yield(canSubjectHandle(subject = forwardedInfo?.subject ?: subject))
        }
        if (canHandle.any { it }.not()) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)

        val payDate = run date@{
            val dateString = lines
                .firstOrNull { it.startsWith("請求日: ") }
                ?.substringAfter("請求日: ")
                ?.substringBefore("日")
                ?: return@date null
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d")
            LocalDateTime.of(
                LocalDate.parse(dateString, dateFormatter),
                LocalTime.MIN,
            )
        }
        return lines.filter { it.startsWith("金額: ¥") }
            .map {
                val price = ParseUtil.getInt(it)
                MoneyUsage(
                    title = displayName,
                    price = price,
                    description = "",
                    service = MoneyUsageServiceType.Microsoft,
                    dateTime = payDate ?: forwardedInfo?.date ?: date,
                )
            }
    }

    private fun canFromHandle(
        from: String,
    ): Boolean {
        return from == "microsoft-noreply@microsoft.com"
    }

    private fun canSubjectHandle(
        subject: String,
    ): Boolean {
        return subject.contains("請求書")
    }
}
