package net.matsudamper.money.backend.feature.session

import java.time.OffsetDateTime
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.host
import io.ktor.server.util.toGMTDate
import net.matsudamper.money.backend.base.CookieManager
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.session.SessionCookieKeys

class KtorCookieManager(
    private val call: ApplicationCall,
) : CookieManager {
    private val host get() = ServerEnv.domain ?: call.request.host()

    override fun setAdminSession(
        idValue: String,
        expires: OffsetDateTime,
    ) {
        setCookie(
            key = SessionCookieKeys.ADMIN_SESSION_ID,
            value = idValue,
            expires = expires,
        )
    }

    override fun setUserSession(
        idValue: String,
        expires: OffsetDateTime,
    ) {
        setCookie(
            key = SessionCookieKeys.USER_SESSION_ID,
            value = idValue,
            expires = expires,
        )
    }

    override fun getAdminSessionId(): String? {
        return call.request.cookies[SessionCookieKeys.ADMIN_SESSION_ID]
    }

    override fun getUserSessionId(): String? {
        return call.request.cookies[SessionCookieKeys.USER_SESSION_ID]
    }

    override fun clearAdminSession() {
        setCookie(
            key = SessionCookieKeys.ADMIN_SESSION_ID,
            value = "",
            expires = OffsetDateTime.now(),
        )
    }

    override fun clearUserSession() {
        setCookie(
            key = SessionCookieKeys.USER_SESSION_ID,
            value = "",
            expires = OffsetDateTime.now(),
        )
    }

    private fun setCookie(
        key: String,
        value: String,
        expires: OffsetDateTime,
    ) {
        call.response.cookies.append(
            name = key,
            value = value,
            expires = expires.toInstant().toGMTDate(),
            domain = host,
            path = "/",
            httpOnly = true,
            secure = ServerEnv.isSecure,
            extensions = mapOf(
                "SameSite" to "Strict",
            ),
        )
    }
}
