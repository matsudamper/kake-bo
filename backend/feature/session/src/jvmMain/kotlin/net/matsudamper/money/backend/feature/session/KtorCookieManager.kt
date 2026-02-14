package net.matsudamper.money.backend.feature.session

import java.time.OffsetDateTime
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.host
import io.ktor.server.util.toGMTDate
import net.matsudamper.money.backend.base.CookieManager
import net.matsudamper.money.backend.base.ServerEnv

class KtorCookieManager(
    private val call: ApplicationCall,
) : CookieManager {
    private val host get() = ServerEnv.domain ?: call.request.host()

    override fun setAdminSession(
        idValue: String,
        expires: OffsetDateTime,
    ) {
        setCookie(
            key = ADMIN_SESSION_ID_KEY,
            value = idValue,
            expires = expires,
        )
    }

    override fun setUserSession(
        idValue: String,
        expires: OffsetDateTime,
    ) {
        setCookie(
            key = USER_SESSION_ID_KEY,
            value = idValue,
            expires = expires,
        )
    }

    override fun getAdminSessionId(): String? {
        return call.request.cookies[ADMIN_SESSION_ID_KEY]
    }

    override fun getUserSessionId(): String? {
        return call.request.cookies[USER_SESSION_ID_KEY]
    }

    override fun clearUserSession() {
        setCookie(
            key = USER_SESSION_ID_KEY,
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
            path = ".",
            secure = ServerEnv.isSecure,
            extensions = mapOf(
                "SameSite" to "Strict",
            ),
        )
    }

    private companion object {
        private const val ADMIN_SESSION_ID_KEY = "admin_session_id"
        private const val USER_SESSION_ID_KEY = "user_session_id"
    }
}
