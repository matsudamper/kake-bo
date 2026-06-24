package net.matsudamper.money.backend.base

public object ServerVariables {
    public const val REDIS_INDEX_CHALLENGE: Int = 2
    public const val REDIS_INDEX_USER_SESSION: Int = 3
    public const val REDIS_INDEX_ADMIN_SESSION: Int = 4
    public const val USER_SESSION_EXPIRE_DAY: Long = 28
    public const val ADMIN_SESSION_EXPIRE_SECONDS: Long = 10 * 60
}
