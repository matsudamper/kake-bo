package net.matsudamper.money.backend.feature.aimailparser

import java.time.LocalDateTime

data class AiMailParseInput(
    val subject: String,
    val from: String,
    val dateTime: LocalDateTime,
    val plain: String?,
    val html: String?,
)
