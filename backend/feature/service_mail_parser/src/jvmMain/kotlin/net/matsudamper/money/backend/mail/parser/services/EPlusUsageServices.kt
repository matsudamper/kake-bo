package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object EPlusUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.EPlus.displayName

    override fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        val forwardedInfo = ParseUtil.parseForwarded(plain)
        if (
            canHandle(
                from = forwardedInfo?.from ?: from,
                subject = forwardedInfo?.subject ?: subject,
            ).not()
        ) {
            return listOf()
        }
        val lines = ParseUtil.splitByNewLine(plain)

        val price = lines.firstOrNull { it.contains("料金合計") }
            ?.let { ParseUtil.getInt(it) }

        val title = "公演名.+?：(.+?)$".toRegex(RegexOption.MULTILINE)
            .find(plain)?.groupValues?.getOrNull(1)
            ?.trim()
        val place = "会場名.+?：(.+?)$".toRegex(RegexOption.MULTILINE)
            .find(plain)?.groupValues?.getOrNull(1)
            ?.trim()

        return listOf(
            MoneyUsage(
                title = title ?: "パースできませんでした",
                price = price,
                description = place.orEmpty(),
                service = MoneyUsageServiceType.EPlus,
                dateTime = forwardedInfo?.date ?: date,
            ),
        )
    }

    private fun canHandle(
        from: String,
        subject: String,
    ): Boolean {
        return from == "info@eplus.co.jp" && subject.contains("当選")
    }
}
