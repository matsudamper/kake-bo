package net.matsudamper.money.backend.mail.parser

import java.time.LocalDateTime
import net.matsudamper.money.backend.mail.parser.services.AmazonCoJpUsageServices
import net.matsudamper.money.backend.mail.parser.services.ESekiReserveUsegeService
import net.matsudamper.money.backend.mail.parser.services.GooglePlayUsageService
import net.matsudamper.money.backend.mail.parser.services.MacdonaldsMobileOrderUsageService
import net.matsudamper.money.backend.mail.parser.services.MovieTicketUsageService
import net.matsudamper.money.backend.mail.parser.services.PayPalUsageService
import net.matsudamper.money.backend.mail.parser.services.RakutenOfflineUsageService
import net.matsudamper.money.backend.mail.parser.services.RakutenOnlineUsageService
import net.matsudamper.money.backend.mail.parser.services.SteamUsageService
import net.matsudamper.money.backend.mail.parser.services.UberEatsUsageService
import net.matsudamper.money.backend.mail.parser.services.YodobashiUsageService

public class MailMoneyUsageParser {
    public fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        return sequenceOf(
            AmazonCoJpUsageServices,
            YodobashiUsageService,
            MovieTicketUsageService,
            SteamUsageService,
            RakutenOfflineUsageService,
            RakutenOnlineUsageService,
            PayPalUsageService,
            MacdonaldsMobileOrderUsageService,
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
                    ).takeIf { it.isNotEmpty() }
                }.onFailure {
                    it.printStackTrace()
                }.getOrNull()
            }
            .firstOrNull()
            .orEmpty()
    }
}
