package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil
import net.matsudamper.money.backend.base.TraceLogger

internal object BoothUsageService : MoneyUsageServices {
    override val displayName: String = "au PAY"

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

        val lines = ParseUtil.splitByNewLine(plain)

        val shopName = run shopName@{
            val labelString = "ショップ名："
            val index = lines.indexOfFirst { it.startsWith(labelString) }
                .takeIf { it >= 0 }
                ?: return@shopName null
            lines.getOrNull(index)?.drop(labelString.length)
        }
        val paymentMethod = run paymentMethod@{
            val targetString = "[決済方法] "
            val index = lines.indexOfFirst { it.startsWith(targetString) }
                .takeIf { it >= 0 }
                ?: return@paymentMethod null

            lines.getOrNull(index)?.drop(targetString.length)
        }
        val totalPrice = run price@{
            val index = lines.indexOfFirst { it.startsWith("合計：¥ ") }
                .takeIf { it >= 0 }
                ?: return@price 0

            val priceString = lines.getOrNull(index) ?: return@price 0
            ParseUtil.getInt(priceString) ?: 0
        }
        val parsedDate = run date@{
            val labelString = "[注文日時] "
            val index = lines.indexOf(labelString)
                .takeIf { it >= 0 }
                ?: return@date null

            // 2024年7月2日 19時29分
            val dateString = lines.getOrNull(index) ?: return@date null

            val temp = runCatching {
                DateTimeFormatterBuilder()
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
                    .optionalStart()
                    .appendLiteral('分')
                    .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                    .optionalStart()
                    .toFormatter()
                    .parse(dateString.trim().drop(labelString.length))
            }.onFailure {
                TraceLogger.impl().noticeThrowable(it, mapOf(), true)
            }.getOrNull()
            LocalDateTime.from(temp)
        }

        val items = buildList title@{
            val startIndex = lines.indexOf("[注文内容]")
                .takeIf { it >= 0 }
                ?: return@title

            var cnt = 1
            val tmpLines = mutableListOf<String>()
            val totalRegex = """= ¥ [,\d]+""".toRegex()
            while (true) {
                val line = lines.getOrNull(startIndex + cnt) ?: break
                if (line.isBlank()) break
                tmpLines.add(line)
                val targetLines = tmpLines.joinToString("")
                if (totalRegex.containsMatchIn(targetLines)) {
                    val item = parseItemLine(
                        line = targetLines,
                        date = parsedDate ?: forwardedInfo?.date ?: date,
                    )
                    if (item != null) {
                        add(item)
                    }
                    tmpLines.clear()
                }
                cnt++
            }

            lines.getOrNull(startIndex + 1)?.trim()
        }

        return buildList {
            add(
                MoneyUsage(
                    title = shopName.orEmpty(),
                    dateTime = parsedDate ?: forwardedInfo?.date ?: date,
                    price = totalPrice,
                    service = MoneyUsageServiceType.Booth,
                    description = buildString {
                        appendLine("決済方法: $paymentMethod")
                        appendLine(
                            items.joinToString("\n") {
                                it.description
                            },
                        )
                    }.trim(),
                ),
            )
            addAll(items)
        }
    }

    private fun parseItemLine(line: String, date: LocalDateTime): MoneyUsage? {
        val totalSplitIndex = line.indexOf("= ¥")
            .takeIf { it >= 0 } ?: return null
        val titleSplitIndex = line.indexOf(": ¥")
            .takeIf { it >= 0 } ?: return null

        val title = line.substring(0, titleSplitIndex).trim()
        val price = ParseUtil.getInt(line.substring(totalSplitIndex, line.length))
            ?: 0
        return MoneyUsage(
            title = title,
            dateTime = date,
            price = price,
            service = MoneyUsageServiceType.Booth,
            description = line,
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "noreply@booth.pm"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject == "ご注文の確認 [BOOTH]"
    }
}
