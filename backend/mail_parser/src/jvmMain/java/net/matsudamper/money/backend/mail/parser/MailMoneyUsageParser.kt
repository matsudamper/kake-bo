package net.matsudamper.money.backend.mail.parser

import java.time.LocalDateTime
import net.matsudamper.money.backend.mail.parser.services.AmazonCoJpUsageServices
import net.matsudamper.money.backend.mail.parser.services.ESekiReserveUsegeService
import net.matsudamper.money.backend.mail.parser.services.GooglePlayUsageService
import net.matsudamper.money.backend.mail.parser.services.MovieTicketUsageService
import net.matsudamper.money.backend.mail.parser.services.PayPalUsageService
import net.matsudamper.money.backend.mail.parser.services.RakutenUsageService
import net.matsudamper.money.backend.mail.parser.services.SteamUsageService
import net.matsudamper.money.backend.mail.parser.services.UberEatsUsageService

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
            RakutenUsageService,
            PayPalUsageService,
            GooglePlayUsageService,
            ESekiReserveUsegeService,
            UberEatsUsageService,
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