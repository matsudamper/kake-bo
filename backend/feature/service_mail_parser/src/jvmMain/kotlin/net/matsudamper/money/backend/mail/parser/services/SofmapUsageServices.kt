package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object SofmapUsageServices : MoneyUsageServices {
    override val displayName: String = "ソフマップ"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)
        val canHandle = sequence {
            yield(canHandleFrom(forwardedInfo?.from ?: from))
            yield(canHandleSubject(forwardedInfo?.subject ?: subject))
        }
        if (canHandle.any { it }.not()) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)

        val orderNumber = lines.firstOrNull { it.trim().startsWith("＜ご注文番号＞") }
            ?.let { lines[lines.indexOf(it) + 1].trim() }
            .orEmpty()

        val totalAmountLine = lines.firstOrNull { it.trim().startsWith("お支払い総額") }
        val totalAmount = totalAmountLine?.let { ParseUtil.getInt(it) }

        val products = extractProducts(lines)

        val description = buildString {
            append("注文番号: $orderNumber\n\n")
            append(products.joinToString("\n") { "${it.name} × ${it.quantity}個" })
        }

        return buildList {
            add(
                MoneyUsage(
                    title = "ソフマップ",
                    price = totalAmount,
                    description = description,
                    service = MoneyUsageServiceType.Sofmap,
                    dateTime = forwardedInfo?.date ?: date,
                ),
            )

            products.forEach { product ->
                add(
                    MoneyUsage(
                        title = product.name,
                        price = null,
                        description = "${product.quantity}個",
                        service = MoneyUsageServiceType.Sofmap,
                        dateTime = forwardedInfo?.date ?: date,
                    ),
                )
            }
        }
    }

    private fun extractProducts(lines: List<String>): List<Product> {
        return buildList {
            val startIndex = lines.indexOfFirst { it.trim().startsWith("＜商     品     名＞") }
                .takeIf { it >= 0 } ?: return@buildList

            val endIndex = lines.drop(startIndex + 1)
                .indexOfFirst { it.startsWith("━") }
                .takeIf { it >= 0 }
                ?.plus(startIndex + 1) ?: return@buildList

            lines.subList(startIndex + 1, endIndex).forEach { line ->
                if (line.isBlank()) return@forEach
                val trimmed = line.trim()
                val match = Regex("""^[・･](.+?)\s+(\d+)個$""").find(trimmed) ?: return@forEach
                val itemName = match.groupValues[1].trim()
                val quantity = match.groupValues[2].toIntOrNull() ?: return@forEach
                add(
                    Product(
                        name = itemName,
                        quantity = quantity,
                    ),
                )
            }
        }
    }

    private data class Product(
        val name: String,
        val quantity: Int,
    )

    private fun canHandleSubject(
        subject: String,
    ): Boolean {
        return subject.contains("お申込ありがとうございます")
    }

    private fun canHandleFrom(
        from: String,
    ): Boolean {
        return from == "support@cc.sofmap.com"
    }
}
