package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object YodobashiUsageService : MoneyUsageServices {
    override val displayName: String = "ヨドバシカメラ"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)
        val canHandle = sequence {
            yield(canHandledWithFrom(forwardedInfo?.from ?: from))
            yield(canHandledWithSubject(forwardedInfo?.subject ?: subject))
            yield(canHandledWithPlain(plain))
        }
        if (canHandle.any { it }.not()) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)

        val totalPrice = run price@{
            "【ご注文金額】今回のお買い物合計金額(.+?)$".toRegex(RegexOption.MULTILINE).find(plain)
                ?.groupValues
                ?.getOrNull(1)
                .orEmpty()
                .mapNotNull { it.toString().toIntOrNull() }
                .joinToString("")
                .toIntOrNull()
        }

        val parsedDate = run date@{
            val result = """・ご注文日.+?(\d+)年(\d+)月(\d+)日"""".toRegex().find(plain)?.groupValues ?: return@date null
            val year = result.getOrNull(1)?.toIntOrNull() ?: return@date null
            val month = result.getOrNull(2)?.toIntOrNull() ?: return@date null
            val day = result.getOrNull(3)?.toIntOrNull() ?: return@date null
            LocalDateTime.of(
                LocalDate.of(year, month, day),
                LocalTime.MIN,
            )
        }

        val orderItemsLines = run {
            val firstIndex = lines.indexOfFirst { it.contains("【ご注文商品】") }.takeIf { it >= 0 }!!
            val endIndex = lines.indexOfFirst { it.contains("ご注文・出荷状況については下記をご確認ください。") }.takeIf { it >= 0 }!!
            lines.subList(firstIndex, endIndex)
        }

        return buildList prices@{
            run total@{
                add(
                    MoneyUsage(
                        title = displayName,
                        dateTime = parsedDate ?: date,
                        price = totalPrice,
                        service = MoneyUsageServiceType.Yodobashi,
                        description = orderItemsLines.joinToString("\n"),
                    ),
                )
            }
            val titleRegex = "^・「(.+)$".toRegex()
            val priceRegex = "合計.+?点(.+?)円".toRegex()

            var beforeTitle: String? = null
            orderItemsLines.forEachIndexed { index, line ->
                val titleResult = titleRegex.find(line)?.groupValues?.getOrNull(1)
                val priceResult = priceRegex.find(line)?.groupValues?.getOrNull(1)
                    ?.mapNotNull { it.toString().toIntOrNull() }
                    ?.joinToString("")
                    ?.toIntOrNull()

                if (titleResult != null) {
                    val capturedBeforeTitle = beforeTitle
                    if (capturedBeforeTitle != null) {
                        add(
                            MoneyUsage(
                                title = capturedBeforeTitle,
                                dateTime = parsedDate ?: date,
                                price = null,
                                service = MoneyUsageServiceType.Yodobashi,
                                description = "",
                            ),
                        )
                    }

                    beforeTitle = titleResult
                        .plus(
                            orderItemsLines.drop(index + 1)
                                .takeWhile { it.isNotBlank() }
                                // remove "　　"
                                .joinToString("") { it.drop(2) },
                        )
                        .dropLast(1) // remove "」"
                } else if (priceResult != null) {
                    add(
                        MoneyUsage(
                            title = beforeTitle.orEmpty(),
                            dateTime = parsedDate ?: date,
                            price = priceResult,
                            service = MoneyUsageServiceType.Yodobashi,
                            description = "",
                        ),
                    )
                    beforeTitle = null
                }
            }
        }
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "thanks_gonyuukin@yodobashi.com"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.contains("ヨドバシ・ドット・コム：クレジットカード決済のご利用確認が完了いたしました")
    }

    private fun canHandledWithPlain(plain: String): Boolean {
        return plain.contains("ヨドバシ・ドット・コムをご利用いただき、ありがとうございます。")
    }
}
