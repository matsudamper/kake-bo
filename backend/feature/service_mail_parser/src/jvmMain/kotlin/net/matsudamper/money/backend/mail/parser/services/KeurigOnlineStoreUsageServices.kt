package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object KeurigOnlineStoreUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.KeurigOnlineStore.displayName

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

        val orderNumberLine = lines.firstOrNull { it.startsWith("ご注文番号：") }
        val orderNumber = orderNumberLine?.substringAfter("ご注文番号：")?.trim().orEmpty()

        val orderDateLine = lines.firstOrNull { it.startsWith("ご注文日時：") }
        val orderDate = orderDateLine?.substringAfter("ご注文日時：")?.trim().orEmpty()

        val paymentMethodLine = lines.firstOrNull { it.startsWith("お支払い方法：") }
        val paymentMethod = paymentMethodLine?.substringAfter("お支払い方法：")?.trim().orEmpty()

        val totalAmountLine = lines.filter { it.startsWith("お支払い合計：") }.lastOrNull()
        val totalAmount = if (totalAmountLine != null) ParseUtil.getInt(totalAmountLine.substringAfter("お支払い合計：")) else null

        val products = extractProducts(lines)

        val description = buildString {
            append("注文番号: $orderNumber\n")
            append("注文日時: $orderDate\n")
            append("支払方法: $paymentMethod\n")
        }

        return buildList {
            add(
                MoneyUsage(
                    title = MoneyUsageServiceType.KeurigOnlineStore.displayName,
                    price = totalAmount,
                    description = description,
                    service = MoneyUsageServiceType.KeurigOnlineStore,
                    dateTime = forwardedInfo?.date ?: date,
                ),
            )

            products.forEach { product ->
                add(
                    MoneyUsage(
                        title = product.name,
                        price = product.price,
                        description = "単価: ${product.price}円 × ${product.quantity}個",
                        service = MoneyUsageServiceType.KeurigOnlineStore,
                        dateTime = forwardedInfo?.date ?: date,
                    ),
                )
            }
        }
    }

    private fun extractProducts(lines: List<String>): List<Product> {
        val productSectionStart = lines.indexOfFirst { it.contains("ご注文商品明細") }
            .takeIf { it >= 0 } ?: return listOf()

        val productLines = lines.subList(productSectionStart + 1, lines.size)

        return buildList {
            var currentName: String? = null
            var currentPrice: Int? = null
            var currentQuantity: Int? = null

            for (line in productLines) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("商品名：") -> {
                        if (currentName != null) {
                            add(Product(name = currentName, price = currentPrice ?: 0, quantity = currentQuantity ?: 0))
                            currentPrice = null
                            currentQuantity = null
                        }
                        currentName = trimmed.substringAfter("商品名：").trim()
                    }
                    trimmed.startsWith("単価：") -> {
                        currentPrice = ParseUtil.getInt(trimmed.substringAfter("単価："))
                    }
                    trimmed.startsWith("数量：") -> {
                        currentQuantity = ParseUtil.getInt(trimmed.substringAfter("数量："))
                    }
                    trimmed.startsWith("小 計：") || trimmed.startsWith("小計：") -> {
                        if (currentName != null) {
                            add(Product(name = currentName, price = currentPrice ?: 0, quantity = currentQuantity ?: 0))
                            currentName = null
                            currentPrice = null
                            currentQuantity = null
                        }
                        break
                    }
                }
            }

            if (currentName != null) {
                add(Product(name = currentName, price = currentPrice ?: 0, quantity = currentQuantity ?: 0))
            }
        }
    }

    private data class Product(
        val name: String,
        val price: Int,
        val quantity: Int,
    )

    private fun canHandleSubject(subject: String): Boolean {
        return subject.contains("キューリグオンラインストア") && subject.contains("ご注文ありがとうございます")
    }

    private fun canHandleFrom(from: String): Boolean {
        return from == "info@keurig.jp"
    }
}
