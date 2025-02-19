package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices

public object MovieTicketUsageService : MoneyUsageServices {
    override val displayName: String = "ムビチケ"

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

        val title = run {
            val regex = "・作品名：「(.+?)」".toRegex()
            regex.find(plain)?.groupValues?.getOrNull(1)
        }

        val price = run {
            val regex = "・合計金額：(.+?)円".toRegex()
            regex.find(plain)?.groupValues?.getOrNull(1)
                ?.map { it.toString().toIntOrNull() }
                ?.filterNotNull()
                ?.joinToString("")
                ?.toIntOrNull()
        }

        val parsedDate = run {
            val regex = """ご購入日時　(\d{4})/(\d{2})/(\d{2}) (\d{2}):(\d{2})""".toRegex()
            val result = regex.find(plain) ?: return@run null

            val year = result.groupValues.getOrNull(1)?.toIntOrNull() ?: return@run null
            val month = result.groupValues.getOrNull(2)?.toIntOrNull() ?: return@run null
            val day = result.groupValues.getOrNull(3)?.toIntOrNull() ?: return@run null
            val hour = result.groupValues.getOrNull(4)?.toIntOrNull() ?: return@run null
            val minute = result.groupValues.getOrNull(5)?.toIntOrNull() ?: return@run null

            LocalDateTime.of(
                LocalDate.of(year, month, day),
                LocalTime.of(hour, minute),
            )
        }

        return listOf(
            MoneyUsage(
                title = title ?: displayName,
                price = price,
                description = "",
                service = MoneyUsageServiceType.MovieTicket,
                dateTime = parsedDate ?: date,
            ),
        )
    }

    private fun canHandledWithPlain(plain: String): Boolean {
        return plain.contains("この度はムビチケをご購入いただきまして、誠にありがとうございます。")
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.contains("【ムビチケ】 ご購入チケット情報")
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "info@movieticket.jp"
    }
}
