package net.matsudamper.money.backend.mail.parser

import java.time.LocalDateTime
import net.matsudamper.money.backend.base.element.MoneyUsageServiceType

public data class MoneyUsage(
    val title: String,
    val description: String,
    val dateTime: LocalDateTime,
    val price: Int?,
    val service: MoneyUsageServiceType,
)
