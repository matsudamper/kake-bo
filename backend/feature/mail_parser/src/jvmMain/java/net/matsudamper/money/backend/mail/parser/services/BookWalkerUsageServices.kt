package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object BookWalkerUsageServices : MoneyUsageServices {
    override val displayName: String = "Book Walker"

    override fun parse(subject: String, from: String, html: String, plain: String, date: LocalDateTime): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)
        val orderDate = forwardedInfo?.date ?: date

        val canHandle = sequence {
            yield(canHandle(from = from, subject = subject))

            yield(
                run forwarded@{
                    if (forwardedInfo != null) {
                        val forwardedSubject = forwardedInfo.subject ?: return@forwarded false
                        val forwardedFrom = forwardedInfo.from ?: return@forwarded false
                        canHandle(from = forwardedFrom, subject = forwardedSubject)
                    } else {
                        false
                    }
                },
            )
        }
        if (canHandle.any { it }.not()) return emptyList()

        val lines = ParseUtil.splitByNewLine(plain)

        val orderList = run {
            val startIndex = lines.indexOfFirst { it == "【ご注文内容】" }
                .takeIf { it >= 0 }!!
                .plus(2)
            val endIndex = lines.drop(startIndex)
                .indexOfFirst { it.startsWith("━━━━━━━━━━━━━━━━━") }
                .takeIf { it >= 0 }!!
                .plus(startIndex)

            lines.subList(startIndex, endIndex)
        }

        return buildList {
            run total@{
                val totalAmountIndex = lines.indexOfFirst { it.startsWith("■お支払合計") }
                    .takeIf { it >= 0 }!!
                val totalAmountLine = lines[totalAmountIndex]
                val amount = ParseUtil.getInt(totalAmountLine)!!

                add(
                    MoneyUsage(
                        title = displayName,
                        price = amount,
                        description = orderList.joinToString("\n").trim(),
                        service = MoneyUsageServiceType.BookWalker,
                        dateTime = orderDate,
                    ),
                )
            }
        }
    }

    private fun canHandle(from: String, subject: String): Boolean {
        return from == "noreply@bookwalker.jp" && subject == "[BOOK☆WALKER] お支払い完了のお知らせ / Order Confirmation"
    }
}
