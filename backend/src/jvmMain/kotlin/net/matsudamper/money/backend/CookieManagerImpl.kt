package net.matsudamper.money.backend

import java.time.OffsetDateTime
import io.ktor.http.CookieEncoding
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.host
import io.ktor.server.util.toGMTDate
import net.matsudamper.money.backend.base.CookieManager
import net.matsudamper.money.backend.base.ServerEnv

internal class CookieManagerImpl(
    private val call: ApplicationCall,
) : CookieManager {
    private val host get() = ServerEnv.domain ?: call.request.host()

    override fun setAdminSession(idValue: String, expires: OffsetDateTime) {
        setCookie(
            key = CookieKeys.adminSessionId,
            value = idValue,
            expires = expires,
        )
    }

    override fun setUserSession(idValue: String, expires: OffsetDateTime) {
        setCookie(
            key = CookieKeys.userSessionId,
            value = idValue,
            expires = expires,
        )
    }

    override fun getAdminSessionId(): String? {
        return call.request.cookies[CookieKeys.adminSessionId]
    }

    override fun getUserSessionId(): String? {
        return call.request.cookies[CookieKeys.userSessionId]
    }

    override fun clearUserSession() {
        setCookie(
            key = CookieKeys.userSessionId,
            value = "",
            expires = OffsetDateTime.MIN,
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
            path = ".",
            secure = ServerEnv.isSecure,
            extensions = mapOf(
                "SameSite" to "Strict",
            ),
        )
    }
}
