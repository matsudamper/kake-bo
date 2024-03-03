package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object FanzaDojinUsageServices : MoneyUsageServices {
    override val displayName: String = "Fanza同人"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        return parsePlain(
            // plainが無く、htmlにplainが含まれている場合がある
            plain = plain.takeIf { it.isNotBlank() } ?: html,
            date = date,
        )
    }

    private fun parsePlain(
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val canHandle =
            sequence {
                yield(canHandledWithPlain(plain))
            }
        if (canHandle.any { it }.not()) return listOf()

        val price =
            run price@{
                val result =
                    "購入額.+?：.+?(.+?)円".toRegex().find(plain)
                        ?.groupValues?.getOrNull(1)!!

                ParseUtil.getInt(result)
            }

        val description =
            run description@{
                val lines = ParseUtil.splitByNewLine(plain)

                val startLineIndex =
                    lines.indexOfFirst { it.startsWith("購入日") }
                        .takeIf { it >= 0 } ?: return@description ""

                val startBuyTextIndex =
                    (
                        lines.drop(startLineIndex + 1)
                            .indexOfFirst { it == "購入商品" }
                            .takeIf { it >= 0 } ?: return@description ""
                    )
                        .plus(startLineIndex + 1)

                val endLineIndex =
                    lines.drop(startBuyTextIndex + 1)
                        .indexOfFirst { it.isBlank() }
                        .plus(startBuyTextIndex + 1)

                lines.subList(startLineIndex, endLineIndex)
                    .joinToString("\n")
            }

        return listOf(
            MoneyUsage(
                title = displayName,
                price = price,
                description = description,
                service = MoneyUsageServiceType.FanzaDojin,
                dateTime = date,
            ),
        )
    }

    private fun canHandledWithPlain(plain: String): Boolean {
        return plain.contains("FANZA同人をご利用いただきありがとうございます。")
    }
}
