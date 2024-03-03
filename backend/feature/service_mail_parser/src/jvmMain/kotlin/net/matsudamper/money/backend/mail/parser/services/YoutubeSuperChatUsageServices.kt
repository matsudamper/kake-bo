package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object YoutubeSuperChatUsageServices : MoneyUsageServices {
    override val displayName: String = "YouTube Membership"

    override fun parse(subject: String, from: String, html: String, plain: String, date: LocalDateTime): List<MoneyUsage> {
        val forwardedOriginInfo = ParseUtil.parseForwarded(plain)
        val canHandle = sequence {
            yield(canHandledWithFrom(forwardedOriginInfo?.from ?: from))
            yield(canHandledWithSubject(forwardedOriginInfo?.subject ?: subject))
        }
        if (canHandle.all { it }.not()) return listOf()

        val plainLines = ParseUtil.splitByNewLine(plain)

        val description = run {
            val index = plainLines.indexOfFirst { it == "Membership details" }
                .takeIf { it >= 0 }!!
            val endIndex = index + plainLines.drop(index + 1).indexOfFirst { it.isBlank() }
            plainLines.subList(index, endIndex).joinToString("\n")
        }

        val title: String = run {
            val itemPrefix = "Item: "
            val index = plainLines.indexOfFirst { it.startsWith(itemPrefix) }
                .takeIf { it >= 0 } ?: return@run forwardedOriginInfo?.subject ?: subject

            plainLines[index].drop(itemPrefix.length)
        }

        val price = run {
            val itemPrefix = "Price: "
            val index = plainLines.indexOfFirst { it.startsWith(itemPrefix) }
                .takeIf { it >= 0 } ?: return@run 0

            ParseUtil.getInt(plainLines[index])
        }

        return listOf(
            MoneyUsage(
                title = title,
                price = price,
                description = description,
                service = MoneyUsageServiceType.YouTubeMembership,
                dateTime = forwardedOriginInfo?.date ?: date,
            ),
        )
    }

    private fun canHandledWithFrom(from: String): Boolean {
        return from == "noreply-purchases@youtube.com"
    }

    private fun canHandledWithSubject(subject: String): Boolean {
        return subject.startsWith("Your membership to")
    }
}
