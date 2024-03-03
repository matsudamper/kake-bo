package net.matsudamper.money.backend.app.interfaces.element

import java.time.LocalDateTime

data class AdminSession(
    val adminSessionId: AdminSessionId,
    val expire: LocalDateTime,
)
