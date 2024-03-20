package net.matsudamper.money.backend.base

public object ServerEnv {
    public val isSecure: Boolean get() = System.getenv()["IS_SECURE"].toBoolean()
    public val domain: String? get() = System.getenv()["DOMAIN"]
    public val port: Int get() = System.getenv()["PORT"]!!.toInt()
    public val frontPath: String get() = System.getenv()["HTML_PATH"] ?: "../frontend/jsApp/build/dist/js/developmentExecutable"
    public val htmlPath: String get() = "$frontPath/index.html"
    public val isDebug: Boolean get() = System.getenv()["IS_DEBUG"]?.toBooleanStrictOrNull() ?: false

    public val dbHost: String get() = System.getenv()["DB_HOST"]!!
    public val dbPort: String get() = System.getenv()["DB_PORT"]!!
    public val dbSchema: String get() = System.getenv()["DB_SCHEMA"]!!
    public val dbUserName: String get() = System.getenv()["DB_USERNAME"]!!
    public val dbPassword: String get() = System.getenv()["DB_PASSWORD"]!!

    public val adminPassword: String get() = System.getenv()["ADMIN_PASSWORD"]!!

    public val userPasswordPepper: String get() = System.getenv("USER_PASSWORD_PEPPER")!!

    public val enableRedis: Boolean get() = System.getenv("ENABLE_REDIS")?.toBooleanStrictOrNull() ?: false
    public val redisHost: String? get() = System.getenv("REDIS_HOST")?.takeIf { it.isNotBlank() }
    public val redisPort: Int? get() = System.getenv("REDIS_PORT")?.toInt()
}
