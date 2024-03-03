package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object YoutubeMembershipUsageServices : MoneyUsageServices {
    override val displayName: String = "YouTube"

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

        var price: Int
        var priceDescription: String?
        run {
            val index =
                plainLines.indexOfFirst { it == "*Total*" }
                    .takeIf { it >= 0 }
            if (index == null) {
                price = 0
                priceDescription = null
                return@run
            }

            val paidLine = plainLines.getOrNull(index + 1)
            if (paidLine == null) {
                price = 0
                priceDescription = null
                return@run
            }

            price = """\*¥(.+?)\*""".toRegex()
                .find(paidLine)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: 0
            priceDescription =
                plainLines.subList(index + 1, index + 3)
                    .joinToString("\n")
                    .takeIf { it.isNotBlank() }
        }

        val channel =
            getNextLine(plainLines) {
                it == "You've successfully made a Super Chat purchase from YouTube on the channel:"
            }
        val title =
            getNextLine(plainLines) {
                it == "YouTube Super Chat"
            }

        return listOf(
            MoneyUsage(
                title = "YouTube Super Chat: ${channel.orEmpty()}",
                price = price,
                description =
                    buildString {
                        if (title != null) {
                            appendLine(title)
                        }
                        if (priceDescription != null) {
                            appendLine(priceDescription)
                        }
                    }.trim(),
                service = MoneyUsageServiceType.YouTube,
                dateTime = forwardedInfo?.date ?: date,
            ),
        )
    }

    private fun getNextLine(
        lines: List<String>,
        block: (String) -> Boolean,
    ): String? {
        val index =
            lines.indexOfFirst { block(it) }
                .takeIf { it >= 0 } ?: return null

        return lines.getOrNull(index + 1)
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "noreply-purchases@youtube.com"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.startsWith("Your YouTube Super Chat receipt")
    }
}
