package net.matsudamper.money.backend

import java.time.LocalDateTime
import net.matsudamper.money.backend.app.interfaces.element.UserSessionId
import net.matsudamper.money.element.UserId

class SessionInfo(
    val userId: UserId,
    val sessionName: String,
    val sessionId: UserSessionId,
    val latestAccess: LocalDateTime,
)
