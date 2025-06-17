package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object KaldiCoffeeFarmUsageServices : MoneyUsageServices {
    override val displayName: String = "カルディコーヒーファーム"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)
        if (
            canHandleSubject(
                subject = forwardedInfo?.subject ?: subject,
            ).not()
        ) {
            return listOf()
        }

        val lines = ParseUtil.splitByNewLine(plain)

        val orderNumberLine = lines.firstOrNull { it.contains("[注文番号]") }
        val orderNumber = orderNumberLine?.substringAfter("[注文番号]")?.trim().orEmpty()

        val orderDateLine = lines.firstOrNull { it.contains("[ご注文日]") }
        val orderDate = orderDateLine?.substringAfter("[ご注文日]")?.trim().orEmpty()

        val paymentMethodLine = lines.firstOrNull { it.contains("[お支払方法]") }
        val paymentMethod = paymentMethodLine?.substringAfter("[お支払方法]")?.trim().orEmpty()

        val totalAmountLine = lines.firstOrNull { it.contains("・お支払金額") }
        val totalAmountString = totalAmountLine?.substringAfter("・お支払金額")?.trim().orEmpty()
        val totalAmount = ParseUtil.getInt(totalAmountString)

        val products = extractProducts(plain)

        val description = buildString {
            append("注文番号: $orderNumber\n")
            append("注文日: $orderDate\n")
            append("支払方法: $paymentMethod\n\n")

            if (products.isNotEmpty()) {
                append("商品一覧:\n")
                products.forEach { product ->
                    append("- ${product.name}\n")
                    append("  単価: ${product.price}円 × ${product.quantity}個 = ${product.totalPrice}円\n")
                }
            }
        }

        return buildList {
            add(
                MoneyUsage(
                    title = "カルディコーヒーファーム オンラインストア",
                    price = totalAmount,
                    description = description,
                    service = MoneyUsageServiceType.KaldiCoffeeFarm,
                    dateTime = forwardedInfo?.date ?: date,
                ),
            )

            products.forEach { product ->
                add(
                    MoneyUsage(
                        title = product.name,
                        price = product.totalPrice,
                        description = "単価: ${product.price}円 × ${product.quantity}個\n注文番号: $orderNumber",
                        service = MoneyUsageServiceType.KaldiCoffeeFarm,
                        dateTime = forwardedInfo?.date ?: date,
                    ),
                )
            }
        }
    }

    private fun extractProducts(plain: String): List<Product> {
        val lines = ParseUtil.splitByNewLine(plain)

        val startIndex = lines.indexOfFirst { it.contains("[ご注文商品]") }.takeIf { it >= 0 }
        if (startIndex == null) return listOf()

        val endIndex = lines.subList(startIndex + 1, lines.size).indexOfFirst {
            it.contains("[配送日指定]") || it.contains("[配送時間帯指定]") || it.trim().isEmpty()
        }.takeIf { it >= 0 }

        if (endIndex == null) return listOf()

        val productSection = lines.subList(startIndex + 1, startIndex + 1 + endIndex)

        return buildList {
            var currentProduct: Product? = null

            for (line in productSection) {
                when {
                    line.startsWith("・商品名（商品コード）") -> {
                        if (currentProduct != null) {
                            add(currentProduct)
                        }

                        val nameCodePattern = "・商品名（商品コード） (.+?)（(.+?)）".toRegex()
                        val match = nameCodePattern.find(line)

                        if (match != null) {
                            val name = match.groupValues[1].trim()
                            val code = match.groupValues[2].trim()
                            currentProduct = Product(name = name, code = code, price = 0, quantity = 0, totalPrice = 0)
                        }
                    }

                    line.startsWith("・単価") -> {
                        val pricePattern = "・単価 (.+?)円".toRegex()
                        val match = pricePattern.find(line)

                        if (match != null && currentProduct != null) {
                            val price = ParseUtil.getInt(match.groupValues[1]) ?: 0
                            currentProduct = currentProduct.copy(price = price)
                        }
                    }

                    line.startsWith("・数量") -> {
                        val quantityPattern = "・数量 (\\d+)".toRegex()
                        val match = quantityPattern.find(line)

                        if (match != null && currentProduct != null) {
                            val quantity = ParseUtil.getInt(match.groupValues[1]) ?: 0
                            currentProduct = currentProduct.copy(quantity = quantity)
                        }
                    }

                    line.startsWith("・商品代") -> {
                        val totalPricePattern = "・商品代 (\\d+)円".toRegex()
                        val match = totalPricePattern.find(line)

                        if (match != null && currentProduct != null) {
                            val totalPrice = ParseUtil.getInt(match.groupValues[1]) ?: 0
                            currentProduct = currentProduct.copy(totalPrice = totalPrice)
                        }
                    }
                }
            }

            if (currentProduct != null) {
                add(currentProduct)
            }
        }
    }

    private data class Product(
        val name: String,
        val code: String,
        val price: Int,
        val quantity: Int,
        val totalPrice: Int,
    )

    private fun canHandleSubject(
        subject: String,
    ): Boolean {
        return subject.contains("カルディコーヒーファーム オンラインストア") &&
                subject.contains("ご注文を受け付けました")
    }
}
