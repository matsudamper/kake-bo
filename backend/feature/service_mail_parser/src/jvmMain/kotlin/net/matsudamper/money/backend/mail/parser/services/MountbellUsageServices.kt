package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object MountbellUsageServices : MoneyUsageServices {
    override val displayName: String = "モンベル"

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardOriginal = ParseUtil.parseForwarded(plain)

        val canHandle =
            sequence {
                yield(canHandledWithFrom(forwardOriginal?.from ?: from))
                yield(canHandledWithSubject(forwardOriginal?.subject ?: subject))
            }
        if (canHandle.any { it.not() }) return listOf()

        val lines = ParseUtil.splitByNewLine(plain)

        val productIndex =
            lines.indexOf("【注文商品】")
                .takeIf { it >= 0 }
        val results = mutableListOf<MoneyUsage>()
        run {
            var cursorIndex = productIndex ?: return@run

            while (true) {
                val productSection =
                    lines.drop(cursorIndex).let {
                        it.take(it.indexOf("").takeIf { it >= 0 } ?: 0)
                    }

                val nameLine =
                    productSection
                        .firstOrNull { it.startsWith("商品名") }
                        ?.dropWhile { it != '：' }?.drop(1)?.trim() ?: break
                val priceLine =
                    productSection
                        .firstOrNull { it.startsWith("単価(税込)") }
                        ?.dropWhile { it != '：' }?.drop(1)
                        ?.takeWhile { it != '円' }?.trim() ?: break
                val countLine =
                    productSection
                        .firstOrNull { it.startsWith("数量") }
                        ?.dropWhile { it != '：' }?.drop(1)?.trim() ?: break

                results.add(
                    MoneyUsage(
                        title = nameLine,
                        price = (ParseUtil.getInt(priceLine) ?: 0) * (ParseUtil.getInt(countLine) ?: 1),
                        description = productSection.joinToString("\n"),
                        service = MoneyUsageServiceType.Mountbell,
                        dateTime = date,
                    ),
                )
                cursorIndex += productSection.size
            }
        }

        return results
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "shopping@montbell.com"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.startsWith("ご注文内容の確認")
    }
}
