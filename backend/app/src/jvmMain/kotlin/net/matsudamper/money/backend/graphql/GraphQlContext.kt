package net.matsudamper.money.backend.graphql

import java.time.LocalDateTime
import java.time.ZoneOffset
import net.matsudamper.money.backend.SessionInfo
import net.matsudamper.money.backend.app.interfaces.element.AdminSession
import net.matsudamper.money.backend.app.interfaces.element.UserSessionId
import net.matsudamper.money.backend.base.CookieManager
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.element.UserId

internal class GraphQlContext(
    private val cookieManager: CookieManager,
    public val dataLoaders: DataLoaders,
    public val userSessionManager: UserSessionManagerImpl,
    public val diContainer: DiContainer,
) {
    private var adminSession: AdminSession? = null

    fun verifyAdminSession() {
        if (adminSession != null) return

        val adminSessionString = cookieManager.getAdminSessionId() ?: throw GraphqlMoneyException.SessionNotVerify()

        val adminSession = diContainer.createAdminUserSessionRepository().verifySession(adminSessionString)
            ?: throw GraphqlMoneyException.SessionNotVerify()
        this.adminSession = adminSession

        cookieManager.setAdminSession(
            idValue = adminSession.adminSessionId.id,
            expires = adminSession.expire.atOffset(ZoneOffset.UTC),
        )
    }

    fun verifyUserSessionAndGetUserId(): UserId = userSessionManager.verifyUserSession()
    fun verifyUserSessionAndGetSessionInfo(): SessionInfo {
        val userId = userSessionManager.verifyUserSession()
        val sessionId = UserSessionId(cookieManager.getUserSessionId()!!)
        val currentSessionInfo = diContainer.createUserSessionRepository()
            .getSessionInfo(sessionId)
            ?: throw GraphqlMoneyException.SessionNotVerify()

        return SessionInfo(
            userId = userId,
            sessionName = currentSessionInfo.name,
            latestAccess = currentSessionInfo.latestAccess,
            sessionId = sessionId,
        )
    }

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
