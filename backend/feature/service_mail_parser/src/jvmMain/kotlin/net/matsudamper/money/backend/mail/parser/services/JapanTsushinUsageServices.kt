package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object JapanTsushinUsageServices : MoneyUsageServices {
    override val displayName: String = "日本通信"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)
        val canHandle =
            sequence {
                yield(canHandledWithFrom(forwardedInfo?.from ?: from))
                yield(canHandledWithSubject(forwardedInfo?.subject ?: subject))
            }
        if (canHandle.all { it }.not()) return listOf()

        val plainLines = ParseUtil.splitByNewLine(plain)

        val number =
            run {
                val index =
                    plainLines.indexOfFirst { it == "電話番号" }
                        .takeIf { it >= 0 } ?: return@run null
                plainLines.getOrNull(index + 1)
            }
        val price =
            run {
                val index =
                    plainLines.indexOfFirst { it == "合計" }
                        .takeIf { it >= 0 } ?: return@run null
                ParseUtil.getInt(plainLines.getOrNull(index + 1) ?: return@run null)
            }
        val dataAmount =
            run {
                val index =
                    plainLines.indexOfFirst { it == "使用データ量" }
                        .takeIf { it >= 0 } ?: return@run null
                plainLines.getOrNull(index + 1)
            }
        val name =
            buildString run@{
                append("日本通信")

                val index =
                    plainLines.indexOfFirst { it == "商品名" }
                        .takeIf { it >= 0 } ?: return@run
                val text = plainLines.getOrNull(index + 1) ?: return@run
                append(":$text")
            }
        return listOf(
            MoneyUsage(
                title = name,
                price = price,
                description =
                buildString {
                    if (number != null) {
                        appendLine("電話番号: $number")
                    }
                    if (dataAmount != null) {
                        appendLine("使用データ量: $dataAmount")
                    }
                }.trim(),
                service = MoneyUsageServiceType.JapanTsushin,
                dateTime = forwardedInfo?.date ?: date,
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "helpdesk@j-com.co.jp"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.endsWith("月額基本料金/通話料/音声オプション料などのお知らせ")
    }
}
