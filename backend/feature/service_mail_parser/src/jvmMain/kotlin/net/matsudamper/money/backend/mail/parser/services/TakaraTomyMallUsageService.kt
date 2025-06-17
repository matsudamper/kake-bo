package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object TakaraTomyMallUsageService : MoneyUsageServices {
    override val displayName: String = "タカラトミーモール"

    private val fromHost = "タカラトミーモール"
    private val subjectRegex = Regex("""^\[タカラトミーモール]ご注文を受け付けました$""")
    private val plainKeywords = listOf(
        "タカラトミー公式ショッピングサイト「タカラトミーモール」でのご利用、誠にありがとうございました。",
        "【オーダーID】",
    )

    override fun canHandledWithFrom(from: String): Boolean {
        return from.contains(fromHost)
    }

    override fun canHandledWithSubject(subject: String): Boolean {
        return subjectRegex.matches(subject)
    }

    override fun canHandledWithPlain(plain: String): Boolean {
        return plainKeywords.all { keyword -> plain.contains(keyword) }
    }

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val lines = ParseUtil.splitByNewLine(plain)

        val orderId = lines.firstNotNullOfOrNull { line ->
            line.trim().removePrefix("【オーダーID】").takeIf { it != line.trim() }
        } ?: return emptyList()

        val orderDateStr = lines.firstNotNullOfOrNull { line ->
            line.trim().removePrefix("【ご注文日】").takeIf { it != line.trim() }
        } ?: return emptyList()

        val orderDate = runCatching {
            val (year, month, day) = orderDateStr.removeSuffix("日")
                .split("年", "月")
                .map { it.toInt() }
            LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.MIN)
        }.getOrNull() ?: return emptyList()

        val totalAmountStr = lines.firstNotNullOfOrNull { line ->
            line.trim().removePrefix("注文金額合計：").removeSuffix("円（税込）").takeIf { it != line.trim() }
        }
        val totalAmount = ParseUtil.getInt(totalAmountStr ?: "") ?: return emptyList()

        val paymentMethod = lines.firstNotNullOfOrNull { line ->
            line.trim().removePrefix("【お支払方法】").takeIf { it != line.trim() }
        } ?: return emptyList()

        val usages = mutableListOf<MoneyUsage>()

        usages.add(
            MoneyUsage(
                title = "タカラトミーモール",
                description = "オーダーID: $orderId",
                service = MoneyUsageServiceType.TakaraTomyMall,
                dateTime = orderDate,
                price = totalAmount,
                category = null, // TODO
            ),
        )

        var itemSectionStarted = false
        val itemRegex = Regex("""^\d+\.(.+)$""")
        val priceRegex = Regex("""価格：([\d,]+)円（税込） x 数量：(\d+) = 合計：([\d,]+)円（税込）""")

        var currentItemName: String? = null
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine == "【ご注文明細】") {
                itemSectionStarted = true
                continue
            }
            if (trimmedLine == "【お買上金額】") {
                itemSectionStarted = false
                break
            }

            if (itemSectionStarted) {
                val itemMatch = itemRegex.find(trimmedLine)
                if (itemMatch != null) {
                    currentItemName = itemMatch.groupValues[1].trim()
                } else if (currentItemName != null) {
                    val priceMatch = priceRegex.find(trimmedLine)
                    if (priceMatch != null) {
                        val name = currentItemName
                        val price = ParseUtil.getInt(priceMatch.groupValues[1]) ?: 0
                        val quantity = ParseUtil.getInt(priceMatch.groupValues[2]) ?: 0
                        // val itemTotal = ParseUtil.getInt(priceMatch.groupValues[3]) ?: 0 // Not used for now

                        usages.add(
                            MoneyUsage(
                                title = name,
                                description = "オーダーID: $orderId",
                                service = MoneyUsageServiceType.TakaraTomyMall,
                                dateTime = orderDate,
                                price = price * quantity,
                                category = null, // TODO
                            ),
                        )
                        currentItemName = null // Reset for next item
                    }
                }
            }
        }

        return usages.toList()
    }
}
