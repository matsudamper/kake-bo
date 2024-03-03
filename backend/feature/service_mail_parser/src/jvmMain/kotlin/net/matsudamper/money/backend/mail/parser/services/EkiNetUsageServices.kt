package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object EkiNetUsageServices : MoneyUsageServices {
    override val displayName: String = "えきネット"

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
                yield(canHandled(from = from, subject = subject))
                yield(
                    run {
                        if (forwardedInfo != null) {
                            val forwardedFrom = forwardedInfo.from ?: return@run false
                            val forwardedSubject = forwardedInfo.subject ?: return@run false
                            canHandled(from = forwardedFrom, subject = forwardedSubject)
                        } else {
                            false
                        }
                    },
                )
            }
        if (canHandle.any { it }.not()) return listOf()
        val lines = ParseUtil.splitByNewLine(plain)

        val trainInfo = getTrainInfo(lines)
        val section = getSection(lines)
        val price = getPrice(lines)

        return listOf(
            MoneyUsage(
                title = section,
                price = price,
                description = trainInfo,
                service = MoneyUsageServiceType.EkiNet,
                dateTime = forwardedInfo?.date ?: date,
            ),
        )
    }

    private fun getPrice(lines: List<String>): Int {
        val index =
            lines.indexOf("■お支払い総額")
                .takeIf { it >= 0 }!!
                .plus(1)

        return ParseUtil.getInt(lines[index])!!
    }

    private fun getSection(lines: List<String>): String {
        val ticketInfo =
            run description@{
                val startIndex =
                    lines.indexOf("==乗車券情報==")
                        .takeIf { it >= 0 }!!
                        .plus(1)

                val endIndex =
                    lines.subList(startIndex, lines.size)
                        .indexOf("")
                        .takeIf { it >= 0 }!!
                        .plus(startIndex)
                lines.subList(startIndex, endIndex)
            }
        return ticketInfo.associate {
            val (key, value) = it.split("：")
            key to value
        }["区　間"]!!
    }

    private fun getTrainInfo(lines: List<String>): String {
        val startIndex =
            lines.indexOf("==列車情報==")
                .takeIf { it >= 0 }!!
                .plus(1)

        val endIndex =
            lines.subList(startIndex, lines.size)
                .indexOf("")
                .takeIf { it >= 0 }!!
                .plus(startIndex)
        return lines.subList(startIndex, endIndex)
            .joinToString("\n")
    }

    private fun canHandled(
        from: String,
        subject: String,
    ): Boolean {
        return from == "reservation@eki-net.com" && subject.contains("【申込完了】申込内容（JRきっぷ）のご案内")
    }
}
