package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object ShunsuguUsageService : MoneyUsageServices {
    override val displayName: String = "旬すぐ"

    override fun parse(subject: String, from: String, html: String, plain: String, date: LocalDateTime): List<MoneyUsage> {
        val canHandle = sequence {
            yield(canHandledWithFrom(from))
            yield(canHandledWithSubject(subject))
            yield(canHandledWithPlain(plain))
        }
        if (canHandle.any { it }.not()) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)

        val price = run price@{
            ParseUtil.getInt(
                "お支払合計金額.*?：(.+?)円".toRegex(RegexOption.MULTILINE)
                    .find(plain)
                    ?.groupValues?.getOrNull(1) ?: return@price null,
            )
        }

        val description = run description@{
            val startIndex = lines.indexOfFirst { it.contains("【お買い上げ金額】") }
            val endIndex = (
                lines.drop(startIndex + 1).indexOfFirst { it.contains("----------") }
                    .takeIf { it >= 0 } ?: return@description ""
                )
                .plus(startIndex + 1)

            lines.subList(startIndex + 1, endIndex)
                .joinToString("\n")
        }

        return listOf(
            MoneyUsage(
                title = displayName,
                price = price,
                description = description,
                service = MoneyUsageServiceType.Shunsugu,
                dateTime = date,
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "info@shunsugu.jp"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject == "【旬をすぐに】ご注文内容のご確認"
    }

    private fun canHandledWithPlain(plain: String): Boolean {
        return plain.contains("この度は、『旬をすぐに』をご注文いただきまして、誠にありがとうございます。")
    }
}
