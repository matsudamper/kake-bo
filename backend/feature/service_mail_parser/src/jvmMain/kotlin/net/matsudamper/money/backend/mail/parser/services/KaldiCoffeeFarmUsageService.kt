package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object KaldiCoffeeFarmUsageService : MoneyUsageServices {
    override val displayName: String = "カルディコーヒーファーム"

    private val subjectRegex = Regex("【ご注文を受け付けました】")
    private val fromAddress = "noreply@kaldi.co.jp"

    private val orderDateRegex = Regex("""\[ご注文日]\s*(\d{4}年\d{2}月\d{2}日)""")
    private val totalAmountRegex = Regex("""\[お支払金額]\s*([\d,]+)円""")
    private val itemNameRegex = Regex("""・商品名（商品コード）\s*(.+?)\s*（\d+）""")
    private val itemPriceRegex = Regex("""・単価\s*([\d,]+)円(?:（税込/\d+%）)?""")
    private val itemQuantityRegex = Regex("""・数量\s*(\d+)""")

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        if (!canHandle(subject, from)) {
            return emptyList()
        }

        val orderDate = parseOrderDate(plain, date)
        val totalAmount = parseTotalAmount(plain)
        val items = parseItems(plain, orderDate)

        val description = if (items.isNotEmpty()) {
            items.joinToString("\n") { "${it.title} (数量: ${it.description})" }
        } else {
            "詳細はメールをご確認ください"
        }

        val title = displayName

        val price = totalAmount ?: if (items.isNotEmpty()) items.sumOf { it.price ?: 0 } else null

        if (price == null && items.isEmpty()) {
            return emptyList()
        }

        return listOf(
            MoneyUsage(
                title = title,
                price = price,
                description = description,
                service = MoneyUsageServiceType.KaldiCoffeeFarm, // This will be added in a later step
                dateTime = orderDate
            )
        )
    }

    private fun canHandle(subject: String, from: String): Boolean {
        val actualFromAddress = ParseUtil.getFromAddress(from)
        return subject.contains(subjectRegex) && actualFromAddress.equals(fromAddress, ignoreCase = true)
    }

    private fun parseOrderDate(plainText: String, fallbackDate: LocalDateTime): LocalDateTime {
        val matchResult = orderDateRegex.find(plainText)
        return matchResult?.groupValues?.get(1)?.let { dateStr ->
            val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.JAPANESE)
            try {
                java.time.LocalDate.parse(dateStr, formatter).atStartOfDay(fallbackDate.zone).toLocalDateTime()
            } catch (e: Exception) {
                fallbackDate
            }
        } ?: fallbackDate
    }

    private fun parseTotalAmount(plainText: String): Int? {
        val matchResult = totalAmountRegex.find(plainText)
        return matchResult?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
    }

    private fun parseItems(plainText: String, orderDateTime: LocalDateTime): List<MoneyUsage> {
        val items = mutableListOf<MoneyUsage>()
        val lines = plainText.lines()

        var currentIndex = 0
        while (currentIndex < lines.size) {
            val line = lines[currentIndex]
            if (line.startsWith("・商品名（商品コード）")) {
                val itemNameMatch = itemNameRegex.find(line)
                val itemName = itemNameMatch?.groupValues?.get(1)?.trim()

                if (itemName != null) {
                    var price: Int? = null
                    var quantity: Int? = null
                    var itemBlockEndIndex = currentIndex + 1

                    for (j in currentIndex + 1 until lines.size) {
                        val nextLine = lines[j]
                        if (nextLine.startsWith("・商品名（商品コード）") || nextLine.startsWith("[配送日指定]")) {
                            itemBlockEndIndex = j
                            break
                        }
                        itemBlockEndIndex = j + 1
                    }

                    for (k in currentIndex + 1 until itemBlockEndIndex) {
                        val detailLine = lines[k]
                        if (price == null) {
                            val itemPriceMatch = itemPriceRegex.find(detailLine)
                            if (itemPriceMatch != null) {
                                price = itemPriceMatch.groupValues[1].replace(",", "")?.toIntOrNull()
                            }
                        }
                        if (quantity == null) {
                            val itemQuantityMatch = itemQuantityRegex.find(detailLine)
                            if (itemQuantityMatch != null) {
                                quantity = itemQuantityMatch.groupValues[1].toIntOrNull()
                            }
                        }
                        if (price != null && quantity != null) break
                    }

                    if (price != null && quantity != null) {
                        items.add(
                            MoneyUsage(
                                title = itemName,
                                price = price * quantity,
                                description = quantity.toString(),
                                service = MoneyUsageServiceType.KaldiCoffeeFarm, // To be added later
                                dateTime = orderDateTime
                            )
                        )
                    }
                }
                currentIndex = itemBlockEndIndex
            } else {
                currentIndex++
            }
        }
        return items
    }
}
