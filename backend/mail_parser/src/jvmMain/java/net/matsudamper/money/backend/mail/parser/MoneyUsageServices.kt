package net.matsudamper.money.backend.mail.parser

import java.time.LocalDateTime

public interface MoneyUsageServices {
    public val displayName: String

    public fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
        date: LocalDateTime,
    ): MoneyUsage?
}
