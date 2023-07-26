package net.matsudamper.money.backend.mail.parser

import java.time.LocalDateTime
import net.matsudamper.money.backend.mail.parser.services.AmazonCoJpUsageServices
import net.matsudamper.money.backend.mail.parser.services.MovieTicketUsageService
import net.matsudamper.money.backend.mail.parser.services.SteamUsageService

public class MailMoneyUsageParser {
    public fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): MoneyUsage? {
        return sequenceOf(
            AmazonCoJpUsageServices,
            MovieTicketUsageService,
            SteamUsageService,
        )
            .mapNotNull {
                runCatching {
                    it.parse(
                        subject = subject,
                        from = from,
                        html = html,
                        plain = plain,
                        date = date,
                    )
                }.onFailure {
                    it.printStackTrace()
                }.getOrNull()
            }
            .firstOrNull()
    }
}