package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object RakutenOnlineUsageService : MoneyUsageServices {
    override val displayName: String = "Rakuten Pay"

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
            yield(canHandledWithHtml(html))
        }
        if (canHandle.any { it }.not()) return listOf()

        val title =
            sequence {
                yield(
                    run title@{
                        val result =
                            "この度は提携サイト「(.+?)」にて楽天ペイ".toRegex(RegexOption.MULTILINE)
                                .find(html)
                                ?.groupValues?.getOrNull(1) ?: return@title null

                        ParseUtil.removeHtmlTag(result)
                    },
                )
                yield(
                    run title@{
                        "この度は提携サイト「\\*(.+?)\\*」にて楽天ペイ".toRegex(RegexOption.MULTILINE)
                            .find(plain)
                            ?.groupValues?.getOrNull(1) ?: return@title null
                    },
                )
            }.filterNotNull().firstOrNull() ?: return listOf()

        val price =
            run price@{
                val index = html.indexOf("小計：").takeIf { it >= 0 } ?: return listOf()

                val result =
                    """>(.+?)円<""".toRegex(RegexOption.MULTILINE)
                        .find(html.drop(index))

                ParseUtil.getInt(
                    result?.groupValues?.getOrNull(1) ?: return@price null,
                )
            }

        val dateTimeResult =
            run date@{
                val index = html.indexOf("ご注文日：").takeIf { it >= 0 } ?: return@date date
                val result =
                    """(\d+)-(\d+)-(\d+) (\d+):(\d+):(\d+)""".toRegex(RegexOption.MULTILINE)
                        .find(html.drop(index))?.groupValues ?: return@date date
                val year = result.getOrNull(1)?.toIntOrNull() ?: return@date date
                val month = result.getOrNull(2)?.toIntOrNull() ?: return@date date
                val day = result.getOrNull(3)?.toIntOrNull() ?: return@date date
                val hour = result.getOrNull(4)?.toIntOrNull() ?: return@date date
                val minute = result.getOrNull(5)?.toIntOrNull() ?: return@date date
                val second = result.getOrNull(6)?.toIntOrNull() ?: return@date date
                LocalDateTime.of(
                    LocalDate.of(year, month, day),
                    LocalTime.of(hour, minute, second),
                )
            }

        return listOf(
            MoneyUsage(
                title = title,
                description = "",
                dateTime = dateTimeResult,
                price = price,
                service = MoneyUsageServiceType.RakutenPay,
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "order@checkout.rakuten.co.jp"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.contains("楽天ペイ 注文受付")
    }

    private fun canHandledWithHtml(plain: String): Boolean {
        return plain.startsWith("楽天ペイ（オンライン決済）ご利用内容確認メール")
    }
}
