package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil
import org.jsoup.Jsoup

internal object SquareEnixMogStationUsageServices : MoneyUsageServices {
    override val displayName: String = "FF XIV"

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
        val products = buildList {
            val productPrefix = "商品名 ："
            val productFirstLines = lines.withIndex().filter { it.value.startsWith(productPrefix) }
            for (productFirstLine in productFirstLines) {
                val name = lines.getOrNull(productFirstLine.index)
                    ?.drop(productPrefix.length) ?: continue
                val price = lines.getOrNull(productFirstLine.index + 1)
                    ?.let { ParseUtil.getInt(it) }
                    ?: continue
                val description = lines.getOrNull(productFirstLine.index + 2)
                    ?: continue

                add(
                    MoneyUsage(
                        title = name,
                        price = price,
                        description = description,
                        service = MoneyUsageServiceType.FFXIV,
                        dateTime = date,
                    ),
                )
            }
        }
        val totalPrice = lines
            .firstOrNull { it.startsWith("合計料金") }
            ?.let { ParseUtil.getInt(it) }
        return buildList {
            add(
                MoneyUsage(
                    title = "FF XIV",
                    price = totalPrice,
                    description = products.joinToString("\n") {
                        "${it.title} ${it.price}円"
                    },
                    service = MoneyUsageServiceType.FFXIV,
                    dateTime = date,
                ),
            )
            addAll(products)
        }
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "autoinfo_jp@account.square-enix.com"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.contains("FFXIV 決済手続き完了")
    }
}
