package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object FFXIVUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.FFXIV.displayName

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
        if (canHandle.any { it }.not()) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)
        val date = run date@{
            val dayPrefix = "ご利用日 ： "
            val dateLine = lines.firstOrNull { it.startsWith(dayPrefix) } ?: return@date null
            val dateString = dateLine.drop(dayPrefix.length)
            DateTimeFormatter.ofPattern("yyyy/MM/dd").parse(dateString, LocalDate::from)
        }

        val price = run price@{
            val pricePrefix = "お支払い金額 ： "
            val priceLine = lines.firstOrNull { it.startsWith(pricePrefix) } ?: return@price null
            val priceString = priceLine.drop(pricePrefix.length)
            ParseUtil.getInt(priceString)
        }

        val cycle = run description@{
            val prefix = "契約周期 ： "
            val cycleLine = lines.firstOrNull { it.startsWith(prefix) } ?: return@description null
            cycleLine.drop(prefix.length)
        }

        return listOf(
            MoneyUsage(
                title = "FFXIV $cycle",
                description = "",
                dateTime = LocalDateTime.of(date, LocalTime.MIDNIGHT),
                price = price,
                service = MoneyUsageServiceType.FFXIV,
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "autoinfo_jp@account.square-enix.com"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.contains("FFXIV サービス契約手続き完了")
    }
}
