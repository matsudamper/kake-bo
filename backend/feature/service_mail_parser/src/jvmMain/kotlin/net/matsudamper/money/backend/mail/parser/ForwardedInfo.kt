package net.matsudamper.money.backend.mail.parser

import java.time.LocalDateTime

public data class ForwardedInfo(
    val from: String,
    val subject: String,
    val dateTime: LocalDateTime,
)
