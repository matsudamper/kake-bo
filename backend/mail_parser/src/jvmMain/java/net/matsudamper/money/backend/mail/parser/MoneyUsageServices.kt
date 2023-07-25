package net.matsudamper.money.backend.mail.parser

public interface MoneyUsageServices {
    public val displayName: String

    public fun parse(
        subject: String,
        from: String,
        html: String,
        plain: String,
    ): MoneyUsage?
}
