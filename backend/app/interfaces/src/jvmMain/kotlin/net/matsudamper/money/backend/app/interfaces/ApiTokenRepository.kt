package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.UserId

interface ApiTokenRepository {
    fun registerToken(id: UserId): String
    fun verifyToken(id: UserId, token: String): UserId
}
