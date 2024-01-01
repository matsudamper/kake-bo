package net.matsudamper.money.backend.graphql

import java.time.LocalDateTime
import java.time.ZoneOffset
import net.matsudamper.money.backend.base.CookieManager
import net.matsudamper.money.backend.datasource.db.element.AdminSession
import net.matsudamper.money.backend.datasource.db.repository.AdminSessionRepository
import net.matsudamper.money.backend.di.RepositoryFactory
import net.matsudamper.money.element.UserId

internal class GraphQlContext(
    private val cookieManager: CookieManager,
    public val repositoryFactory: RepositoryFactory,
    public val dataLoaders: DataLoaders,
    public val userSessionManager: UserSessionManagerImpl,
) {
    private var adminSession: AdminSession? = null

    fun verifyAdminSession() {
        if (adminSession != null) return

        val adminSessionString = cookieManager.getAdminSessionId() ?: throw GraphqlMoneyException.SessionNotVerify()

        val adminSession = AdminSessionRepository.verifySession(adminSessionString)
            ?: throw GraphqlMoneyException.SessionNotVerify()
        this.adminSession = adminSession

        cookieManager.setAdminSession(
            idValue = adminSession.adminSessionId.id,
            expires = adminSession.expire.atOffset(ZoneOffset.UTC),
        )
    }

    fun verifyUserSessionAndGetUserId(): UserId = userSessionManager.verifyUserSession()
    fun getSessionInfo() = userSessionManager.getSessionInfo()

    fun clearUserSession() {
        userSessionManager.clearUserSession()
    }

    fun setAdminSessionCookie(value: String, expires: LocalDateTime) {
        cookieManager.setAdminSession(
            idValue = value,
            expires = expires.atOffset(ZoneOffset.UTC),
        )
    }

    fun setUserSessionCookie(value: String, expires: LocalDateTime) {
        cookieManager.setUserSession(
            idValue = value,
            expires = expires.atOffset(ZoneOffset.UTC),
        )
    }
}
