package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object NijisanjiOfficialStoreUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.NijisanjiOfficialStore.displayName

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
            yield(canFromHandle(from = forwardedInfo?.from ?: from))
        }
        if (canHandle.any { it }.not()) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)

        return buildList {
            val allTotalStartText = "ご注文金額総合計 "
            val allTotalLine = lines.firstOrNull { it.startsWith(allTotalStartText) }
                ?.drop(allTotalStartText.length)
                ?.let { ParseUtil.getInt(it) }

            add(
                MoneyUsage(
                    title = MoneyUsageServiceType.NijisanjiOfficialStore.displayName,
                    price = allTotalLine ?: 0,
                    description = "合計",
                    service = MoneyUsageServiceType.NijisanjiOfficialStore,
                    dateTime = orderDate,
                ),
            )

            val shippingStartText = "送料 "
            val shippingLine = lines.firstOrNull { it.startsWith(shippingStartText) }
                ?.drop(shippingStartText.length)
                ?.let { ParseUtil.getInt(it) }

            add(
                MoneyUsage(
                    title = "${MoneyUsageServiceType.NijisanjiOfficialStore.displayName}送料",
                    price = shippingLine ?: 0,
                    description = "送料",
                    service = MoneyUsageServiceType.NijisanjiOfficialStore,
                    dateTime = orderDate,
                ),
            )

            val remainingLines = lines.toMutableList()
            while (remainingLines.isNotEmpty()) {
                val productLineStartText = "商品名 "
                val productLine = remainingLines.removeFirst()
                    .takeIf { it.startsWith(productLineStartText) }
                    ?: continue
                val productName = productLine.drop(productLineStartText.length)

                while (remainingLines.isNotEmpty()) {
                    val countStartText = "個数 "
                    val countLine = remainingLines.removeFirst()
                        .takeIf { it.startsWith(countStartText) }
                        ?: continue
                    val count = ParseUtil.getInt(countLine.drop(countStartText.length)) ?: continue

                    while (remainingLines.isNotEmpty()) {
                        val priceStartText = "販売価格 "
                        val priceLine = remainingLines.removeFirst()
                            .takeIf { it.startsWith(priceStartText) }
                            ?: continue
                        val price = ParseUtil.getInt(priceLine.drop(priceStartText.length)) ?: continue

                        while (remainingLines.isNotEmpty()) {
                            val totalStartText = "小計 "
                            val totalLine = remainingLines.removeFirst()
                                .takeIf { it.startsWith(totalStartText) }
                                ?: continue
                            val total = ParseUtil.getInt(totalLine.drop(totalStartText.length)) ?: continue

                            add(
                                MoneyUsage(
                                    title = productName,
                                    price = price,
                                    description = buildString {
                                        appendLine(productName)
                                        appendLine("${price}円 * ${count}個")
                                        appendLine("合計: ${total}円")
                                    }.trim(),
                                    service = MoneyUsageServiceType.NijisanjiOfficialStore,
                                    dateTime = orderDate,
                                ),
                            )
                            break
                        }
                        break
                    }
                    break
                }
            }
        }
    }

    private fun canFromHandle(
        from: String,
    ): Boolean {
        return from == "thanks@shop.nijisanji.jp"
    }
}
