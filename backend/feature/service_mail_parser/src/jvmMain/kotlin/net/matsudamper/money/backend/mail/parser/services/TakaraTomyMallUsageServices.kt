package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object TakaraTomyMallUsageServices : MoneyUsageServices {
    override val displayName: String = "タカラトミーモール"

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

        val orderIdLine = lines.firstOrNull { it.contains("【オーダーID】") }
        val orderId = orderIdLine?.substringAfter("【オーダーID】")?.trim().orEmpty()

        val orderDateLine = lines.firstOrNull { it.contains("【ご注文日】") }
        val orderDate = orderDateLine?.substringAfter("【ご注文日】")?.trim().orEmpty()

        val paymentMethodLine = lines.firstOrNull { it.contains("【お支払方法】") }
        val paymentMethod = paymentMethodLine?.substringAfter("【お支払方法】")?.trim().orEmpty()

        val totalAmountLine = lines.firstOrNull { it.contains("注文金額合計：") }
        val totalAmountString = totalAmountLine?.substringAfter("注文金額合計：")?.substringBefore("（税込）")?.trim().orEmpty()
        val totalAmount = ParseUtil.getInt(totalAmountString)

        val products = extractProducts(plain)

        val description = buildString {
            append("注文ID: $orderId\n")
            append("注文日: $orderDate\n")
            append("支払方法: $paymentMethod\n\n")
        }

        return buildList {
            add(
                MoneyUsage(
                    title = "タカラトミーモール",
                    price = totalAmount,
                    description = description,
                    service = MoneyUsageServiceType.TakaraTomyMall,
                    dateTime = forwardedInfo?.date ?: date,
                ),
            )

            products.forEach { product ->
                add(
                    MoneyUsage(
                        title = product.name,
                        price = product.totalPrice,
                        description = "${product.price}円 × ${product.quantity}個",
                        service = MoneyUsageServiceType.TakaraTomyMall,
                        dateTime = forwardedInfo?.date ?: date,
                    ),
                )
            }
        }
    }

    private fun extractProducts(plain: String): List<Product> {
        val lines = ParseUtil.splitByNewLine(plain)

        return buildList {
            val startLine = lines.indexOfFirst { it.startsWith("・お届け先：") }
                .takeIf { it >= 0 }
                ?.plus(1)
                ?: return@buildList


            val reamingLines = lines.subList(startLine, lines.size).toMutableList()
            while (reamingLines.isNotEmpty()) {
                val nameLine = reamingLines.removeFirstOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: break
                val priceLine = reamingLines.removeFirstOrNull()?.takeIf { it.isNotBlank() } ?: break

                val name = run {
                    val productResult = Regex("\\d+\\.(.+?)$").find(nameLine)
                    productResult?.groupValues?.get(1)?.trim().orEmpty()
                }

                val priceResult = Regex("価格：(.+?)円（税込）.+?数量：(.+?) =(.+?)$").find(priceLine)
                val price = ParseUtil.getInt(priceResult?.groupValues?.get(1)?.trim().orEmpty()) ?: 0
                val quantity = ParseUtil.getInt(priceResult?.groupValues?.get(2)?.trim().orEmpty()) ?: 0
                val totalPrice = ParseUtil.getInt(priceResult?.groupValues?.get(3)?.trim().orEmpty()) ?: 0

                add(
                    Product(
                        name = name,
                        price = price,
                        quantity = quantity,
                        totalPrice = totalPrice,
                    ),
                )
            }
        }
    }

    private data class Product(
        val name: String,
        val price: Int,
        val quantity: Int,
        val totalPrice: Int,
    )

    private fun canHandleSubject(
        subject: String,
    ): Boolean {
        return subject.contains("タカラトミーモール") &&
                subject.contains("ご注文を受け付けました")
    }

    private fun canHandleFrom(
        from: String,
    ): Boolean {
        return from == "no-reply@takaratomymall.jp"
    }
}
