package net.matsudamper.money.backend

import java.time.Instant
import net.matsudamper.money.backend.app.interfaces.element.UserSessionId
import net.matsudamper.money.element.SessionRecordId
import net.matsudamper.money.element.UserId

class SessionInfo(
    val userId: UserId,
    val sessionRecordId: SessionRecordId,
    val sessionName: String,
    val sessionId: UserSessionId,
    val latestAccess: Instant,
)
