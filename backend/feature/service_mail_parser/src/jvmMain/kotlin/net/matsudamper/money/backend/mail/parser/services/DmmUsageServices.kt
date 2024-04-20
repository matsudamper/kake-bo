package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object DmmUsageServices : MoneyUsageServices {
    override val displayName: String = "DMM"

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
        }
        if (canHandle.any { it }.not()) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)
        val parsedDate: LocalDateTime? = run date@{
            val result = "^購入日 ：\\s(\\d+?)年(\\d+?)月(\\d+?)日$"
                .toRegex()
                .find(plain) ?: return@date null

            LocalDateTime.of(
                LocalDate.of(
                    result.groupValues[1].toInt(),
                    result.groupValues[2].toInt(),
                    result.groupValues[3].toInt(),
                ),
                LocalTime.of(0, 0),
            )
        }
        val parsedPrice: Int? = run price@{
            val result = lines.firstOrNull { it.startsWith("合計") }
                ?: return@price null
            ParseUtil.getInt(result)
        }

        return listOf(
            MoneyUsage(
                title = "DMM",
                price = parsedPrice,
                description = "",
                service = MoneyUsageServiceType.Dmm,
                dateTime = parsedDate ?: date,
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "info@mail.dmm.com"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.startsWith("デジタル配信商品 購入完了のお知らせ")
    }
}
