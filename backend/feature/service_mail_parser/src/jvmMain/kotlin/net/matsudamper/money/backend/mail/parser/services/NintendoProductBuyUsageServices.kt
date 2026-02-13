package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object NintendoProductBuyUsageServices : MoneyUsageServices {
    override val displayName: String = "任天堂"

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
        }
        if (canHandle.all { it }.not()) return listOf()

        val plainLines = ParseUtil.splitByNewLine(plain)

        val price = run {
            val priceText = getNextLine(plainLines) {
                it == "お支払い合計金額:"
            } ?: return@run null
            "^(.+?)円".toRegex().find(priceText)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
        }
        val parsedDate = run {
            val dateLine = getNextLine(plainLines) {
                it == "○ご購入日時:"
            } ?: return@run null
            val dateText = "^.+? (.+?)$".toRegex()
                .find(dateLine)
                ?.groupValues
                ?.getOrNull(1)

            val tmp = runCatching {
                DateTimeFormatterBuilder()
                    .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                    .appendLiteral('/')
                    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                    .appendLiteral('/')
                    .appendValue(ChronoField.DAY_OF_MONTH, 2)
                    .appendLiteral(' ')
                    .append(DateTimeFormatter.ISO_LOCAL_TIME)
                    .toFormatter()
                    .parse(dateText)
            }.onFailure {
                it.printStackTrace()
            }.getOrNull() ?: return@run null

            LocalDateTime.from(tmp)
        }
        val device = getNextLine(plainLines) {
            it == "○デバイスタイプ:"
        }
        val name = getNextLine(plainLines) {
            it == "○ご購入商品:"
        } ?: return emptyList()

        return listOf(
            MoneyUsage(
                title = name,
                price = price,
                description = buildString {
                    appendLine(displayName)
                    if (device != null) {
                        appendLine("デバイスタイプ: $device")
                    }
                }.trim(),
                service = MoneyUsageServiceType.Nintendo,
                dateTime = parsedDate ?: forwardedInfo?.date ?: date,
            ),
        )
    }

    private fun getNextLine(
        lines: List<String>,
        block: (String) -> Boolean,
    ): String? {
        val index = lines.indexOfFirst { block(it) }
            .takeIf { it >= 0 } ?: return null

        return lines.getOrNull(index + 1)
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "no-reply@accounts.nintendo.com"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject == "[ご利用明細] 商品のご購入"
    }
}
