package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object NissinOnlineStoreUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.NissinOnlineStore.displayName

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)
        val orderDate = forwardedInfo?.date ?: date

        val canHandle = sequence {
            yield(canHandleFrom(forwardedInfo?.from ?: from))
            yield(canHandleSubject(forwardedInfo?.subject ?: subject))
        }
        if (canHandle.any { it }.not()) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)

        val totalPrice = extractTotal(lines)

        val products = extractProducts(lines)

        val description = buildString {
            for (product in products) {
                appendLine("${product.name} × ${product.quantity}")
            }
        }.trim()

        return buildList {
            add(
                MoneyUsage(
                    title = displayName,
                    price = totalPrice,
                    description = description,
                    service = MoneyUsageServiceType.NissinOnlineStore,
                    dateTime = orderDate,
                ),
            )

            products.forEach { product ->
                add(
                    MoneyUsage(
                        title = product.name,
                        price = product.price,
                        description = "× ${product.quantity}",
                        service = MoneyUsageServiceType.NissinOnlineStore,
                        dateTime = orderDate,
                    ),
                )
            }
        }
    }

    private fun extractTotal(lines: List<String>): Int? {
        val totalIndex = lines.indexOfLast { it.trim() == "合計" }
            .takeIf { it >= 0 } ?: return null

        for (i in (totalIndex + 1) until lines.size) {
            val line = lines[i].trim()
            if (line.isBlank()) continue
            return extractPrice(line)
        }
        return null
    }

    private fun extractProducts(lines: List<String>): List<Product> {
        val startIndex = lines.indexOfFirst { it.trim() == "注文概要" }
            .takeIf { it >= 0 } ?: return listOf()

        val endIndex = run {
            for (i in (startIndex + 1) until lines.size) {
                if (lines[i].trim() == "小計") return@run i
            }
            lines.size
        }

        val productLines = lines.subList(startIndex + 1, endIndex)

        return buildList {
            var i = 0
            while (i < productLines.size) {
                val line = productLines[i].trim()
                val productMatch = "^(.+?)\\s*×\\s*(\\d+)$".toRegex().find(line)
                if (productMatch != null) {
                    val name = productMatch.groupValues[1].trim()
                    val quantity = productMatch.groupValues[2].toIntOrNull() ?: 1

                    var price: Int? = null
                    for (j in (i + 1) until productLines.size) {
                        val priceLine = productLines[j].trim()
                        if ("^(.+?)\\s*×\\s*(\\d+)$".toRegex().matches(priceLine)) break
                        val extracted = extractPrice(priceLine)
                        if (extracted != null) {
                            price = extracted
                            break
                        }
                        if (priceLine == "無料") {
                            price = 0
                            break
                        }
                    }

                    add(Product(name = name, quantity = quantity, price = price))
                }
                i++
            }
        }
    }

    private fun extractPrice(text: String): Int? {
        val cleaned = text.replace("*", "")
        return "[¥￥]([\\d,]+)".toRegex().find(cleaned)
            ?.groupValues?.getOrNull(1)
            ?.let { ParseUtil.getInt(it) }
    }

    private data class Product(
        val name: String,
        val quantity: Int,
        val price: Int?,
    )

    private fun canHandleSubject(subject: String): Boolean {
        return subject.contains("ご注文内容の確認") && subject.contains("注文番号")
    }

    private fun canHandleFrom(from: String): Boolean {
        return from.endsWith("@mail.nissin.com")
    }
}
