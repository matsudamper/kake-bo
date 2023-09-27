package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object PostCoffeeUsageServices : MoneyUsageServices {
    override val displayName: String = "PostCoffee"

    override fun parse(subject: String, from: String, html: String, plain: String, date: LocalDateTime): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)
        val canHandle = sequence {
            yield(canHandled(from = from, subject = subject))
            yield(
                run {
                    if (forwardedInfo != null) {
                        val forwardedFrom = forwardedInfo.from ?: return@run false
                        val forwardedSubject = forwardedInfo.subject ?: return@run false
                        canHandled(from = forwardedFrom, subject = forwardedSubject)
                    } else {
                        false
                    }
                },
            )
        }
        if (canHandle.any { it }.not()) return listOf()
        val lines = ParseUtil.splitByNewLine(plain)

        val price = run {
            val amountTitleIndex = lines.indexOf("■合計金額")
                .takeIf { it >= 0 }
                ?: return listOf()
            val amountLine = lines
                .getOrNull(amountTitleIndex + 1)
                ?: return emptyList()
            ParseUtil.getInt(amountLine)
                ?: return emptyList()
        }

        val description = run description@{
            val startIndex = lines.indexOf("《ご注文内容》")
                .takeIf { it >= 0 }!!
                .plus(1)

            val endIndex = lines.indexOf("■合計金額")
                .takeIf { it >= 0 }!!
                .minus(1)

            lines
                .subList(startIndex, endIndex)
                .joinToString("\n")
        }

        return listOf(
            MoneyUsage(
                title = "PostCoffee",
                price = price,
                description = description,
                service = MoneyUsageServiceType.PostCoffee,
                dateTime = forwardedInfo?.date ?: date,
            ),
        )
    }

    private fun canHandled(from: String, subject: String): Boolean {
        return from == "support@postcoffee.co" && subject.startsWith("[PostCoffee]ご購入ありがとうございます！")
    }
}
