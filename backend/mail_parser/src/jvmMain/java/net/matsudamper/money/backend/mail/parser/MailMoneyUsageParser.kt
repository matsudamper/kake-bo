package net.matsudamper.money.backend.mail.parser

import net.matsudamper.money.backend.mail.parser.services.AmazonCoJpUsageServices

public class MailMoneyUsageParser {
    public fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
    ): MoneyUsage? {
        return sequenceOf(AmazonCoJpUsageServices)
            .map {
                it.parse(
                    subject = subject,
                    from = from,
                    html = html,
                    plain = plain,
                )
            }
            .firstOrNull()
    }
}