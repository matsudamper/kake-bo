package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object RakutenUsageServices : MoneyUsageServices {
    override val displayName: String = "楽天"

    override fun parse(subject: String, from: String, html: String, plain: String, date: LocalDateTime): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)

        val canHandle = sequence {
            yield(canHandle(from = from, subject = subject))

            yield(
                run forwarded@{
                    if (forwardedInfo != null) {
                        val forwardedSubject = forwardedInfo.subject ?: return@forwarded false
                        val forwardedFrom = forwardedInfo.from ?: return@forwarded false
                        canHandle(from = forwardedFrom, subject = forwardedSubject)
                    } else {
                        false
                    }
                },
            )
        }
        if (canHandle.any { it }.not()) return emptyList()

        val lines = ParseUtil.splitByNewLine(plain)

        val storeName = sequence {
            val singleLine = plain.replace("\r\n", "").replace("\n", "")
            yield(
                run {
                    val result = "この度は楽天市場内のショップ「(.+?)」をご利用いただきまして、誠にありがとうございます。".toRegex()
                        .find(singleLine)
                    result?.groupValues?.get(1)
                },
            )
            yield(
                run {
                    val result = "この度は楽天市場内のショップ「\\*(.+?)\\*」をご利用いただきまして、誠にありがとうございます。".toRegex()
                        .find(singleLine)
                    result?.groupValues?.get(1)
                },
            )
        }.filterNotNull().first()

        val totalPrice = run totalPrice@{
            val priceIndex = lines.indexOfFirst { it.startsWith("支払い金額") || it.startsWith("*支払い金額*") }
                .takeIf { it >= 0 }!!
            ParseUtil.getInt(lines[priceIndex])!!
        }

        val orderDate = run orderDate@{
            var prefix: String? = null
            var index: Int? = null
            for (checkPrefix in listOf("[日時]", "注文日時 ")) {
                index = lines.indexOfFirst { it.startsWith(checkPrefix) }
                    .takeIf { it >= 0 } ?: continue
                prefix = checkPrefix
            }
            prefix!!
            index!!

            val temporal = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .parse(lines[index].drop(prefix.length).trim().replace(" ", "T"))

            LocalDateTime.of(
                LocalDate.of(
                    temporal.get(ChronoField.YEAR),
                    temporal.get(ChronoField.MONTH_OF_YEAR),
                    temporal.get(ChronoField.DAY_OF_MONTH),
                ),
                LocalTime.of(
                    temporal.get(ChronoField.HOUR_OF_DAY),
                    temporal.get(ChronoField.MINUTE_OF_HOUR),
                    temporal.get(ChronoField.SECOND_OF_MINUTE),
                ),
            )
        }

        return buildList {
            run total@{
                add(
                    MoneyUsage(
                        title = "[$displayName] $storeName",
                        price = totalPrice,
                        description = storeName,
                        service = MoneyUsageServiceType.Rakuten,
                        dateTime = orderDate,
                    ),
                )
            }
        }
    }

    private fun canHandle(from: String, subject: String): Boolean {
        return from == "order@rakuten.co.jp" && subject == "【楽天市場】注文内容ご確認（自動配信メール）"
    }
}
