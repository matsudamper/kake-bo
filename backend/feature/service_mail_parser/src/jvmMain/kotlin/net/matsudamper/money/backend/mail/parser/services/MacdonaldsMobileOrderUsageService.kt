package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices

internal object MacdonaldsMobileOrderUsageService : MoneyUsageServices {
    override val displayName: String = "マクドナルド"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val canHandle =
            sequence {
                yield(canHandledWithFrom(from))
                yield(canHandledWithPlain(plain))
            }
        if (canHandle.any { it }.not()) return listOf()

        val lines =
            plain.split("\r\n")
                .flatMap { it.split("\n") }

        val description =
            run {
                val first = lines.indexOf("数量 品目 価格").takeIf { it >= 0 } ?: return@run null
                val end =
                    lines.indexOf("上記金額を正に領収いたしました。")
                        .takeIf { it >= 0 }
                        ?.minus(1) ?: return@run null

                lines.subList(first, end)
                    .joinToString("\n")
            }

        val price =
            run price@{
                val result =
                    "^ご請求金額(.+?)$".toRegex(RegexOption.MULTILINE)
                        .find(plain)
                        ?.groupValues?.getOrNull(1)
                        ?: return@price null

                result.mapNotNull { it.toString().toIntOrNull() }
                    .joinToString("")
                    .toIntOrNull()
            }

        return listOf(
            MoneyUsage(
                title = displayName,
                dateTime = date,
                price = price,
                service = MoneyUsageServiceType.Macdonalds,
                description = description.orEmpty(),
            ),
        )
    }

    private fun canHandledWithPlain(plain: String): Boolean {
        return plain.contains("この度は、マクドナルドモバイルオーダーを")
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "noreply@nsp.mdj.jp"
    }
}
