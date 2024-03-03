package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object NttEastBillingUsageServices : MoneyUsageServices {
    override val displayName: String = "@ビリング"

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

        val customerNumber = getDescription(plainLines, "お客さま番号")
        val price =
            run {
                val text = getDescription(plainLines, "ご請求金額") ?: return@run null
                ParseUtil.getInt(text)
            }
        val month = getDescription(plainLines, "ご請求月分")
        return listOf(
            MoneyUsage(
                title = displayName,
                price = price,
                description =
                    buildString {
                        if (customerNumber != null) {
                            appendLine("お客さま番号: $customerNumber")
                        }
                        if (month != null) {
                            appendLine("ご請求月分: $month")
                        }
                    }.trim(),
                service = MoneyUsageServiceType.NttEastAtBilling,
                dateTime = forwardedInfo?.date ?: date,
            ),
        )
    }

    private fun getDescription(
        lines: List<String>,
        title: String,
    ): String? {
        val prefix = "●$title："
        val index =
            lines.indexOfFirst { it.startsWith(prefix) }
                .takeIf { it >= 0 } ?: return null
        val targetLine = lines.getOrNull(index) ?: return null
        return targetLine.drop(prefix.length)
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "information@billing.ntt-east.co.jp"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.endsWith("月分のご利用料金のお知らせ")
    }
}
