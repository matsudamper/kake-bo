package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices

internal object YodobashiUsageService : MoneyUsageServices {
    override val displayName: String = "ヨドバシカメラ"

    override fun parse(subject: String, from: String, html: String, plain: String, date: LocalDateTime): List<MoneyUsage> {
        val canHandle = sequence {
            yield(canHandledWithFrom(from))
            yield(canHandledWithSubject(subject))
            yield(canHandledWithPlain(plain))
        }
        if (canHandle.any { it }.not()) return listOf()

        val price = run price@{
            "【ご注文金額】今回のお買い物合計金額(.+?)$".toRegex(RegexOption.MULTILINE).find(plain)
                ?.groupValues
                ?.getOrNull(1)
                .orEmpty()
                .mapNotNull { it.toString().toIntOrNull() }
                .joinToString("")
                .toIntOrNull()
        }

        val parsedDate = run date@{
            val result = """・ご注文日.+?(\d+)年(\d+)月(\d+)日"""".toRegex().find(plain)?.groupValues ?: return@date null
            val year = result.getOrNull(1)?.toIntOrNull() ?: return@date null
            val month = result.getOrNull(2)?.toIntOrNull() ?: return@date null
            val day = result.getOrNull(3)?.toIntOrNull() ?: return@date null
            LocalDateTime.of(
                LocalDate.of(year, month, day),
                LocalTime.MIN,
            )
        }

        return listOf(
            MoneyUsage(
                title = displayName,
                dateTime = parsedDate ?: date,
                price = price,
                service = MoneyUsageServiceType.Yodobashi,
                description = "",
            )
        )    
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "thanks_gonyuukin@yodobashi.com"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.contains("ヨドバシ・ドット・コム：クレジットカード決済のご利用確認が完了いたしました")
    }

    private fun canHandledWithPlain(plain: String): Boolean {
        return plain.contains("ヨドバシ・ドット・コムをご利用いただき、ありがとうございます。")
    }
}