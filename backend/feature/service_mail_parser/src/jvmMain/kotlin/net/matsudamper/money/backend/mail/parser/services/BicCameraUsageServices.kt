package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object BicCameraUsageServices : MoneyUsageServices {
    override val displayName: String = "ビックカメラ"

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
            yield(canHandle(from = from, subject = subject))

            if (forwardedInfo != null) {
                yield(canForwardedHandle(metadata = forwardedInfo))
            }
        }
        if (canHandle.any { it }.not()) return emptyList()

        val lines = ParseUtil.splitByNewLine(plain)

        val orderList = run {
            val startIndex = lines.indexOfFirst { it == "ご注文商品：" }
                .takeIf { it >= 0 } ?: return@run null
            val endIndex = lines.drop(startIndex).let {
                it.zip(it.drop(1))
            }.indexOfFirst { it.first == "" && it.second == "" }
                .takeIf { it >= 0 }
                ?.plus(startIndex) ?: return@run null

            lines.subList(startIndex + 1, endIndex)
        }

        return buildList {
            run total@{
                val totalAmountIndex = lines.indexOfFirst { it.startsWith("■お支払い金額：") }
                    .takeIf { it >= 0 } ?: return@total

                val totalAmountLine = lines[totalAmountIndex]

                val amount = ParseUtil.getInt(totalAmountLine) ?: return@total

                add(
                    MoneyUsage(
                        title = "ビックカメラ",
                        price = amount,
                        description = orderList?.joinToString("\n").orEmpty(),
                        service = MoneyUsageServiceType.BicCamera,
                        dateTime = orderDate,
                    ),
                )
            }
            orderList.orEmpty().joinToString("\n")
                .split("\n\n")
                .map { it.trim() }
                .forEach { orderItem ->
                    val split = orderItem.split("\n")
                    val name = split.first()
                    val propertyMap = split.associate { line ->
                        line.split("：").let {
                            it.first() to it.drop(1).joinToString("：")
                        }
                    }
                    val price = propertyMap["金額"]!!.let { ParseUtil.getInt(it) }
                    add(
                        MoneyUsage(
                            title = "$name * ${propertyMap["数量"]}個",
                            price = price,
                            description = orderItem,
                            service = MoneyUsageServiceType.BicCamera,
                            dateTime = orderDate,
                        ),
                    )
                }
        }
    }

    private fun canHandle(
        from: String,
        subject: String,
    ): Boolean {
        return from == "support@cc.biccamera.com" && subject == "ご注文ありがとうございます"
    }

    private fun canForwardedHandle(metadata: ParseUtil.MailMetadata): Boolean {
        return metadata.from == "support@cc.biccamera.com" && metadata.subject == "ご注文ありがとうございます"
    }
}
