package net.matsudamper.money.backend.mail.parser

import java.time.LocalDateTime
import net.matsudamper.money.backend.mail.parser.lib.ParseUtil
import net.matsudamper.money.backend.mail.parser.services.AmazonCoJpMonthlyUsageServices
import net.matsudamper.money.backend.mail.parser.services.AmazonCoJpUsageServices
import net.matsudamper.money.backend.mail.parser.services.AuEasyPaymentUsageServices
import net.matsudamper.money.backend.mail.parser.services.AuPayUsageService
import net.matsudamper.money.backend.mail.parser.services.BicCameraUsageServices
import net.matsudamper.money.backend.mail.parser.services.BookWalkerUsageServices
import net.matsudamper.money.backend.mail.parser.services.BoothUsageService
import net.matsudamper.money.backend.mail.parser.services.DmmUsageServices
import net.matsudamper.money.backend.mail.parser.services.ESekiReserveUsegeService
import net.matsudamper.money.backend.mail.parser.services.EkiNetUsageServices
import net.matsudamper.money.backend.mail.parser.services.FanzaDojinUsageServices
import net.matsudamper.money.backend.mail.parser.services.GooglePlayUsageService
import net.matsudamper.money.backend.mail.parser.services.JapanTsushinUsageServices
import net.matsudamper.money.backend.mail.parser.services.MacdonaldsMobileOrderUsageService
import net.matsudamper.money.backend.mail.parser.services.MitsuiSumitomoCardUsageServices
import net.matsudamper.money.backend.mail.parser.services.MountbellUsageServices
import net.matsudamper.money.backend.mail.parser.services.MovieTicketUsageService
import net.matsudamper.money.backend.mail.parser.services.NijisanjiOfficialStoreUsageServices
import net.matsudamper.money.backend.mail.parser.services.NintendoChargeUsageServices
import net.matsudamper.money.backend.mail.parser.services.NintendoProductBuyUsageServices
import net.matsudamper.money.backend.mail.parser.services.NttEastBillingUsageServices
import net.matsudamper.money.backend.mail.parser.services.PayPalUsageService
import net.matsudamper.money.backend.mail.parser.services.PostCoffeeSubscriptionUsageServices
import net.matsudamper.money.backend.mail.parser.services.PostCoffeeUsageServices
import net.matsudamper.money.backend.mail.parser.services.PovoUsageServices
import net.matsudamper.money.backend.mail.parser.services.RakutenCardUsageService
import net.matsudamper.money.backend.mail.parser.services.RakutenOfflineUsageService
import net.matsudamper.money.backend.mail.parser.services.RakutenOnlineUsageService
import net.matsudamper.money.backend.mail.parser.services.RakutenUsageServices
import net.matsudamper.money.backend.mail.parser.services.RentioUsageServices
import net.matsudamper.money.backend.mail.parser.services.ShunsuguUsageService
import net.matsudamper.money.backend.mail.parser.services.SteamUsageService
import net.matsudamper.money.backend.mail.parser.services.UberEatsUsageService
import net.matsudamper.money.backend.mail.parser.services.YMobileUsageServices
import net.matsudamper.money.backend.mail.parser.services.YahooShoppingUsageServices
import net.matsudamper.money.backend.mail.parser.services.YodobashiUsageService
import net.matsudamper.money.backend.mail.parser.services.YoutubeMembershipUsageServices
import net.matsudamper.money.backend.mail.parser.services.YoutubeSuperChatUsageServices

public object MailParser {
    public fun parseUsage(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): List<MoneyUsage> {
        return sequenceOf(
            AmazonCoJpUsageServices,
            AmazonCoJpMonthlyUsageServices,
            YodobashiUsageService,
            MovieTicketUsageService,
            SteamUsageService,
            YahooShoppingUsageServices,
            RakutenOfflineUsageService,
            RakutenOnlineUsageService,
            PayPalUsageService,
            MacdonaldsMobileOrderUsageService,
            GooglePlayUsageService,
            ESekiReserveUsegeService,
            UberEatsUsageService,
            FanzaDojinUsageServices,
            DmmUsageServices,
            ShunsuguUsageService,
            RakutenUsageServices,
            PostCoffeeSubscriptionUsageServices,
            PostCoffeeUsageServices,
            BicCameraUsageServices,
            BookWalkerUsageServices,
            EkiNetUsageServices,
            JapanTsushinUsageServices,
            AuEasyPaymentUsageServices,
            NintendoProductBuyUsageServices,
            NintendoChargeUsageServices,
            YoutubeSuperChatUsageServices,
            YoutubeMembershipUsageServices,
            NttEastBillingUsageServices,
            MountbellUsageServices,
            MitsuiSumitomoCardUsageServices,
            AuPayUsageService,
            BoothUsageService,
            PovoUsageServices,
            NijisanjiOfficialStoreUsageServices,
            RentioUsageServices,
            RakutenCardUsageService,
            YMobileUsageServices,
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

    public fun forwardedInfo(text: String): ForwardedInfo? {
        val info = ParseUtil.parseForwarded(text) ?: return null
        return ForwardedInfo(
            from = info.from ?: return null,
            subject = info.subject ?: return null,
            dateTime = info.date ?: return null,
        )
    }
}
