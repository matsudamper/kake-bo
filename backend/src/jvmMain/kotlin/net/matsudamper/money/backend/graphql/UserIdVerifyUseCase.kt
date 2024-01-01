package net.matsudamper.money.backend.graphql

import java.time.ZoneOffset
import io.ktor.http.CookieEncoding
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.host
import io.ktor.util.date.GMTDate
import net.matsudamper.money.backend.CookieKeys
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.element.UserSessionId
import net.matsudamper.money.backend.repository.UserSessionRepository
import net.matsudamper.money.element.UserId

class UserIdVerifyUseCase(
    private val call: ApplicationCall,
    private val userSessionRepository: UserSessionRepository = UserSessionRepository(),
) {
    private val host get() = ServerEnv.domain ?: call.request.host()
    private var verifyUserSessionResult: UserSessionRepository.VerifySessionResult? = null
    private val cookieExtensions = mapOf(
        "SameSite" to "Strict",
    )
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
        val userSessionString = getCookie(CookieKeys.userSessionId)
        if (userSessionString == null) {
            this.verifyUserSessionResult = UserSessionRepository.VerifySessionResult.Failure
            return UserSessionRepository.VerifySessionResult.Failure
        }

        when (val userSessionResult = userSessionRepository.verifySession(UserSessionId(userSessionString))) {
            is UserSessionRepository.VerifySessionResult.Failure -> {
                this.verifyUserSessionResult = UserSessionRepository.VerifySessionResult.Failure
                return UserSessionRepository.VerifySessionResult.Failure
            }

            is UserSessionRepository.VerifySessionResult.Success -> {
                this.verifyUserSessionResult = userSessionResult

                setCookie(
                    key = CookieKeys.userSessionId,
                    value = userSessionResult.sessionId.id,
                    expires = GMTDate(userSessionResult.expire.toEpochSecond(ZoneOffset.UTC) * 1000),
                )

                return userSessionResult
            }
        }
    }

    /**
     * @return isSuccess
     */
    fun clearUserSession() {
        val sessionId = call.request.cookies[CookieKeys.userSessionId]
        if (sessionId == null) {
            verifyUserSessionResult = null
            return
        }
        userSessionRepository.clearSession(UserSessionId(sessionId))

        verifyUserSessionResult = null
        call.response.cookies.append(
            name = CookieKeys.userSessionId,
            value = "",
            encoding = CookieEncoding.URI_ENCODING,
            maxAge = 0L,
            expires = GMTDate.START,
            domain = null,
            path = null,
            extensions = cookieExtensions,
        )
    }

    private fun getCookie(key: String): String? {
        return call.request.cookies[key]
    }

    fun setCookie(
        key: String,
        value: String,
        expires: GMTDate,
    ) {
        call.response.cookies.append(
            name = key,
            value = value,
            expires = expires,
            domain = host,
            path = ".",
            secure = ServerEnv.isSecure,
            extensions = cookieExtensions,
        )
    }
}
