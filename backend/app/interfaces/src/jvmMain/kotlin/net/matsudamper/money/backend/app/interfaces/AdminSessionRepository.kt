package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.backend.app.interfaces.element.AdminSession

interface AdminSessionRepository {
    fun verifySession(adminSessionId: String): AdminSession?
    fun createSession(): AdminSession
}
