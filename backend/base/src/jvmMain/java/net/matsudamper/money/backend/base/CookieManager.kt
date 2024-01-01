package net.matsudamper.money.backend.base

import java.time.OffsetDateTime

public interface CookieManager {
    public fun setAdminSession(idValue: String, expires: OffsetDateTime)
    public fun setUserSession(idValue: String, expires: OffsetDateTime)
    public fun getAdminSessionId(): String?
    public fun getUserSessionId(): String?
    public fun clearUserSession()
}