package net.matsudamper.money.backend.feature.aimailparser

import java.time.LocalDateTime

data class AiParsedUsage(
    val title: String,
    val description: String,
    val amount: Int?,
    val dateTime: LocalDateTime?,
    val serviceName: String?,
)
