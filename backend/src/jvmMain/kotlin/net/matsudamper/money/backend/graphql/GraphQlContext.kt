package net.matsudamper.money.backend.graphql

import java.time.LocalDateTime
import java.time.ZoneOffset
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.host
import io.ktor.util.date.GMTDate
import net.matsudamper.money.backend.CookieKeys
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.di.RepositoryFactory
import net.matsudamper.money.backend.element.AdminSession
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.backend.element.UserSession
import net.matsudamper.money.backend.repository.AdminSessionRepository
import net.matsudamper.money.backend.repository.UserSessionRepository

class GraphQlContext(
    private val call: ApplicationCall,
    public val repositoryFactory: RepositoryFactory,
    public val dataLoaders: DataLoaders,
) {
    private var adminSession: AdminSession? = null
    private var verifyUserSessionResult: UserSessionRepository.VerifySessionResult.Success? = null
    private val host get() = ServerEnv.domain ?: call.request.host()

    fun verifyAdminSession() {
        if (adminSession != null) return

        val adminSessionString = getCookie(CookieKeys.adminSessionId) ?: throw GraphqlMoneyException.SessionNotVerify()

        val adminSession = AdminSessionRepository.verifySession(adminSessionString)
            ?: throw GraphqlMoneyException.SessionNotVerify()
        this.adminSession = adminSession

        setCookie(
            key = CookieKeys.adminSessionId,
            value = adminSession.adminSessionId.id,
            expires = GMTDate(adminSession.expire.toEpochSecond(ZoneOffset.UTC) * 1000),
        )
    }

    fun verifyUserSession(): UserId {
        val verifyUserSessionResult = verifyUserSessionResult
        if (verifyUserSessionResult != null) return verifyUserSessionResult.userId

        val userSessionString = getCookie(CookieKeys.userSessionId) ?: throw GraphqlMoneyException.SessionNotVerify()

        when (val userSessionResult = UserSessionRepository().verifySession(UserSession(userSessionString))) {
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

    fun setAdminSessionCookie(value: String, expires: LocalDateTime) {
        setCookie(
            key = CookieKeys.adminSessionId,
            value = value,
            expires = GMTDate(expires.toEpochSecond(ZoneOffset.UTC) * 1000),
        )
    }

    fun setUserSessionCookie(value: String, expires: LocalDateTime) {
        setCookie(
            key = CookieKeys.userSessionId,
            value = value,
            expires = GMTDate(expires.toEpochSecond(ZoneOffset.UTC) * 1000),
        )
    }

    fun getCookie(key: String): String? {
        return call.request.cookies[key]
    }

    private fun setCookie(
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
