package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object YahooShoppingUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.YahooShopping.displayName

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardOriginal = ParseUtil.parseForwarded(plain)
        val subject = forwardOriginal?.subject ?: subject
        val canHandle = sequence {
            yield(canHandledWithFrom(forwardOriginal?.from ?: from))
        }
        if (canHandle.any { it.not() }) return listOf()
        return if (subject.contains("お申し込みのご確認")) {
            parseFurusato(
                plain = plain,
                originalDate = forwardOriginal?.date ?: date,
            )
        } else if (subject.contains("ご注文の確認")) {
            parseNormalShopping(
                plain = plain,
                date = forwardOriginal?.date ?: date,
            )
        } else {
            listOf()
        }
    }

    private fun parseFurusato(plain: String, originalDate: LocalDateTime): List<MoneyUsage> {
        val lines = ParseUtil.splitByNewLine(plain)
        val regex = """^(（\d+?）)(.+)$""".toRegex()
        val priceRegex = """^(.+?)円 × (.+?) ＝ (.+?)円$""".toRegex()
        val productLines = lines.withIndex().mapNotNull { (index, line) ->
            index to (regex.find(line) ?: return@mapNotNull null)
        }
        val date = run {
            val dateLine = lines.firstOrNull { it.startsWith("注文日時：") } ?: return@run originalDate

            runCatching {
                DateTimeFormatterBuilder()
                    .appendLiteral("注文日時：")
                    .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                    .appendLiteral('年')
                    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                    .appendLiteral('月')
                    .appendValue(ChronoField.DAY_OF_MONTH, 2)
                    .appendLiteral('日')
                    .appendLiteral(' ')
                    .appendValue(ChronoField.HOUR_OF_DAY, 2)
                    .appendLiteral('時')
                    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                    .appendLiteral('分')
                    .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                    .appendLiteral('秒')
                    .toFormatter()
                    .parse(dateLine)
            }.onFailure {
                TraceLogger.impl().noticeInfo(it)
            }.map {
                LocalDateTime.from(it)
            }.getOrNull() ?: originalDate
        }

        return buildList {
            for ((index, productLine) in productLines) {
                val priceLine = lines.getOrNull(index + 3) ?: continue
                val total = priceRegex.find(priceLine)?.groupValues?.getOrNull(3)
                    ?.let { ParseUtil.getInt(it) }

                val title = productLine.groupValues.getOrNull(2)
                add(
                    MoneyUsage(
                        title = title.orEmpty(),
                        price = total ?: 0,
                        description = priceLine,
                        service = MoneyUsageServiceType.YahooShopping,
                        dateTime = date,
                    ),
                )
            }
        }
    }

    private fun parseNormalShopping(
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val lines = ParseUtil.splitByNewLine(plain)

        val results = mutableListOf<MoneyUsage>()
        for (index in 1 until Int.MAX_VALUE) {
            val productPrefix = "（$index）"
            val productIndex = lines
                .indexOfFirst { it.startsWith(productPrefix) }
                .takeIf { it >= 0 } ?: break

            val priceLine = lines.drop(productIndex)
                .firstOrNull { "＝" in it } ?: break
            val price = ParseUtil.getInt(priceLine.dropWhile { it != '＝' })
            results.add(
                MoneyUsage(
                    title = lines[productIndex].drop(productPrefix.length),
                    price = price ?: 0,
                    description = "",
                    service = MoneyUsageServiceType.YahooShopping,
                    dateTime = date,
                ),
            )
        }
        if (results.isEmpty()) return listOf()
        val storeName = run {
            val storePrefix = "ストア名："
            lines.firstOrNull { it.startsWith(storePrefix) }
                ?.drop(storePrefix.length)
        }

        results.add(
            0,
            MoneyUsage(
                title = storeName ?: displayName,
                price = run price@{
                    val parsedTotal = results.mapNotNull { it.price }.sum()
                    val productTotal = run {
                        val total = lines.firstOrNull { it.startsWith("商品の合計金額") }
                            ?: return@price parsedTotal
                        ParseUtil.getInt(total) ?: return@price parsedTotal
                    }
                    val coupon = run coupon@{
                        val coupon = lines.firstOrNull { it.startsWith("クーポン利用") }
                            ?: return@coupon 0
                        ParseUtil.getInt(coupon) ?: 0
                    }
                    val shipping = run shipping@{
                        val shipping = lines.firstOrNull { it.startsWith("送料") }
                            ?: return@shipping 0
                        ParseUtil.getInt(shipping) ?: 0
                    }
                    val shoppingTicket = run ticket@{
                        val ticket = lines.firstOrNull { it.startsWith("商品券利用") }
                            ?: return@ticket 0
                        ParseUtil.getInt(ticket) ?: 0
                    }
                    productTotal + shipping - coupon - shoppingTicket
                },
                description = run {
                    val startIndex = lines.indexOfFirst { it.startsWith("商品の合計金額") }
                    val endIndex = lines.indexOfFirst { it.startsWith("合計金額") }
                    lines.subList(startIndex, endIndex + 1).joinToString("\n")
                },
                service = MoneyUsageServiceType.YahooShopping,
                dateTime = date,
            ),
        )

        return results
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "shopping-order-master@mail.yahoo.co.jp"
    }
}
