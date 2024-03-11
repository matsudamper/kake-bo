package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.app.interfaces.ApiTokenRepository
import net.matsudamper.money.backend.datasource.db.DbConnection
import net.matsudamper.money.db.schema.tables.JApiTokens
import net.matsudamper.money.element.UserId

class ApiTokenRepositoryImpl(
    private val dbConnection: DbConnection,
) : ApiTokenRepository {
    private val apiTokens = JApiTokens.API_TOKENS
    override fun registerToken(id: UserId): String {
        TODO()
    }

    override fun verifyToken(id: UserId, token: String): UserId {
        TODO("Not yet implemented")
    }
}