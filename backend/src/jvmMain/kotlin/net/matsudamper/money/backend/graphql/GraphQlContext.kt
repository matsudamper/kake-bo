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
import net.matsudamper.money.backend.repository.AdminSessionRepository


class GraphQlContext(
    private val call: ApplicationCall,
    public val repositoryFactory: RepositoryFactory,
    public val dataLoaders: DataLoaders,
    public val userIdVerifyUseCase: UserIdVerifyUseCase,
) {
    private var adminSession: AdminSession? = null
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

    fun verifyUserSession(): UserId = userIdVerifyUseCase.verifyUserSession()

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

    private fun getCookie(key: String): String? {
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
