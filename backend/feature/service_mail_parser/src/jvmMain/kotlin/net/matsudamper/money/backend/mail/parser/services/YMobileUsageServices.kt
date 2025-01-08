package net.matsudamper.money.backend.mail.parser.services

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType
import net.matsudamper.money.backend.mail.parser.MoneyUsage
import net.matsudamper.money.backend.mail.parser.MoneyUsageServices
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil

internal object YMobileUsageServices : MoneyUsageServices {
    override val displayName: String = MoneyUsageServiceType.YMobile.displayName

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
            ).not()
        ) {
            return listOf()
        }
        val useDate = (forwardedInfo?.date ?: date).minusMonths(1)
        return listOf(
            MoneyUsage(
                title = "$displayName${useDate.monthValue}月分料金",
                price = null,
                description = "",
                service = MoneyUsageServiceType.YMobile,
                dateTime = forwardedInfo?.date ?: date,
            ),
        )
    }

    private fun canHandle(
        from: String,
    ): Boolean {
        return from == "billinginfo@mail.my.ymobile.jp"
    }
}
