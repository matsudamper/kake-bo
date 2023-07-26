package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices

internal object RakutenUsageService : MoneyUsageServices {
    override val displayName: String = "Rakuten Pay"

    override fun parse(subject: String, from: String, html: String, plain: String, date: LocalDateTime): MoneyUsage? {
        val canHandle = sequence {
            yield(canHandledWithFrom(from))
            yield(canHandledWithSubject(subject))
            yield(canHandledWithPlain(plain))
        }
        if (canHandle.any { it }.not()) return null

        val price = run price@{
            "決済総額(.+?)$".toRegex(RegexOption.MULTILINE)
                .find(plain)
                ?.groupValues?.getOrNull(1)
                ?.mapNotNull { it.toString().toIntOrNull() }
                ?.joinToString("")
                ?.toIntOrNull()
        }

        val title = run price@{
            "ご利用店舗(.+?)$".toRegex(RegexOption.MULTILINE)
                .find(plain)
                ?.groupValues?.getOrNull(1)
                ?.trimStart()
                ?.trimEnd()
        }

        val parsedDate = run date@{
            val dateText = "ご利用日時(.+?)$".toRegex(RegexOption.MULTILINE)
                .find(plain)
                ?.groupValues?.getOrNull(1)
                ?: return@date null

            val result = """(\d+)/(\d+)/(\d+).+?(\d+):(\d+)""".toRegex().find(dateText)
                ?: return@date null

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

        return MoneyUsage(
            title = "$title",
            price = price,
            description = "",
            service = MoneyUsageServiceType.RakutenPay,
            dateTime = parsedDate ?: date,
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "no-reply@pay.rakuten.co.jp"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject == "楽天ペイアプリご利用内容確認メール"
    }

    private fun canHandledWithPlain(plain: String): Boolean {
        return plain.startsWith("楽天ペイアプリご利用内容確認メール")
    }
}
