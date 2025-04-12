package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object DLSiteUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.DLSite.displayName

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
        }
        if (canHandle.any { it }.not()) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)

        val buyDateString = "購入日時："
        val buyDayItems = lines.fold(mutableListOf<MutableList<String>>()) { result, text ->
            if (text.startsWith(buyDateString)) {
                result.add(mutableListOf())
            }
            if (result.isEmpty()) return@fold result
            result.last().add(text)
            result
        }
        val totalResults = mutableListOf<MoneyUsage>()
        val productResults = mutableListOf<MoneyUsage>()
        for (buyDayItem in buyDayItems) {
            val buyDate = LocalDateTime.from(
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
                    .parse(buyDayItem.first().drop(buyDateString.length)),
            )

            val firstIndex = buyDayItem.indexOfFirst { it.startsWith("1 ") }.takeIf { it >= 0 } ?: continue
            val usages = buildList {
                for ((index, productLine) in buyDayItem.subList(
                    fromIndex = firstIndex,
                    toIndex = buyDayItem.lastIndex + 1,
                ).withIndex()
                ) {
                    if (productLine.startsWith("${index + 1} ").not()) break

                    val matchResult = """^\d+ .+? (.+?) ¥(.+?)$""".toRegex().find(productLine)
                    if (matchResult == null) continue

                    val title = matchResult.groups[1]?.value
                    val priceString = matchResult.groups[2]?.value

                    add(
                        MoneyUsage(
                            title = title.orEmpty(),
                            price = priceString?.let { ParseUtil.getInt(it) },
                            description = productLine,
                            service = MoneyUsageServiceType.DLSite,
                            dateTime = buyDate,
                        ),
                    )
                }
            }
            productResults.addAll(usages)

            val totalPrice = run price@{
                val prefix = "ご請求額：¥"
                val priceString = buyDayItem.getOrNull(1)
                    ?.takeIf { it.startsWith("ご請求額：") }
                    ?.drop(prefix.length) ?: return@price null
                ParseUtil.getInt(priceString)
            }

            totalResults.add(
                MoneyUsage(
                    title = displayName,
                    price = totalPrice,
                    description = usages.joinToString("\n") { "${it.title} ¥${it.price}" },
                    service = MoneyUsageServiceType.DLSite,
                    dateTime = buyDate,
                ),
            )
        }

        return buildList {
            addAll(totalResults)
            addAll(productResults)
        }
    }

    private fun canFromHandle(
        from: String,
    ): Boolean {
        return from == "support@dlsite.com"
    }
}
