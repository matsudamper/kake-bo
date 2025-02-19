package net.matsudamper.money.backend.graphql

import java.time.ZoneOffset
import net.matsudamper.money.backend.app.interfaces.UserSessionRepository
import net.matsudamper.money.backend.app.interfaces.element.UserSessionId
import net.matsudamper.money.backend.base.CookieManager
import net.matsudamper.money.backend.base.ServerVariables
import net.matsudamper.money.element.UserId

internal class UserSessionManagerImpl(
    private val cookieManager: CookieManager,
    private val userSessionRepository: UserSessionRepository,
) {
    private var verifyUserSessionResult: UserSessionRepository.VerifySessionResult? = null

    fun verifyUserSession(): UserId {
        return when (val info = getSessionInfo()) {
            is UserSessionRepository.VerifySessionResult.Failure -> {
                throw GraphqlMoneyException.SessionNotVerify()
            }

            is UserSessionRepository.VerifySessionResult.Success -> {
                info.userId
            }
        }
    }

    fun getSessionInfo(): UserSessionRepository.VerifySessionResult {
        val verifyUserSessionResult = verifyUserSessionResult
        if (verifyUserSessionResult != null) return verifyUserSessionResult
        val userSessionString = cookieManager.getUserSessionId()
        if (userSessionString == null) {
            this.verifyUserSessionResult = UserSessionRepository.VerifySessionResult.Failure
            return UserSessionRepository.VerifySessionResult.Failure
        }

        when (
            val userSessionResult = userSessionRepository.verifySession(
                sessionId = UserSessionId(userSessionString),
                expireDay = ServerVariables.USER_SESSION_EXPIRE_DAY,
            )
        ) {
            is UserSessionRepository.VerifySessionResult.Failure -> {
                this.verifyUserSessionResult = UserSessionRepository.VerifySessionResult.Failure
                return UserSessionRepository.VerifySessionResult.Failure
            }

            is UserSessionRepository.VerifySessionResult.Success -> {
                this.verifyUserSessionResult = userSessionResult

                cookieManager.setUserSession(
                    idValue = userSessionResult.sessionId.id,
                    expires = userSessionResult.latestAccess.atOffset(ZoneOffset.UTC)
                        .plusDays(ServerVariables.USER_SESSION_EXPIRE_DAY),
                )

                return userSessionResult
            }
        }
    }

    /**
     * @return isSuccess
     */
    fun clearUserSession() {
        val sessionId = cookieManager.getUserSessionId()
        if (sessionId == null) {
            verifyUserSessionResult = null
            return
        }
        userSessionRepository.clearSession(UserSessionId(sessionId))

        verifyUserSessionResult = null
        cookieManager.clearUserSession()
    }
}
