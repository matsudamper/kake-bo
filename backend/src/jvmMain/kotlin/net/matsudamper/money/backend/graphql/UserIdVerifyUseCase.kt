package net.matsudamper.money.backend.graphql

import java.time.ZoneOffset
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.host
import io.ktor.util.date.GMTDate
import net.matsudamper.money.backend.CookieKeys
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.backend.element.UserSessionId
import net.matsudamper.money.backend.repository.UserSessionRepository

class UserIdVerifyUseCase(
    private val call: ApplicationCall,
) {
    private val host get() = ServerEnv.domain ?: call.request.host()
    private var verifyUserSessionResult: UserSessionRepository.VerifySessionResult.Success? = null

    fun verifyUserSession(): UserId {
        val verifyUserSessionResult = verifyUserSessionResult
        if (verifyUserSessionResult != null) return verifyUserSessionResult.userId

        val userSessionString = getCookie(CookieKeys.userSessionId) ?: throw GraphqlMoneyException.SessionNotVerify()

        when (val userSessionResult = UserSessionRepository().verifySession(UserSessionId(userSessionString))) {
            is UserSessionRepository.VerifySessionResult.Failure -> {
                throw GraphqlMoneyException.SessionNotVerify()
            }

            is UserSessionRepository.VerifySessionResult.Success -> {
                this.verifyUserSessionResult = userSessionResult

                setCookie(
                    key = CookieKeys.userSessionId,
                    value = userSessionResult.sessionId.id,
                    expires = GMTDate(userSessionResult.expire.toEpochSecond(ZoneOffset.UTC) * 1000),
                )

                return userSessionResult.userId
            }
        }
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
        )
    }
}