package net.matsudamper.money.backend.base

public object ServerEnv {
    public val isSecure: Boolean = System.getenv()["IS_SECURE"].toBoolean()
    public val domain: String? = System.getenv()["DOMAIN"]
    public val port: Int = System.getenv()["PORT"]!!.toInt()
    public val frontPath: String = System.getenv()["HTML_PATH"] ?: "../frontend/jsApp/build/dist/js/developmentExecutable"
    public val htmlPath: String = "$frontPath/index.html"
    public val isDebug: Boolean = System.getenv()["IS_DEBUG"]?.toBooleanStrictOrNull() ?: false

    public val dbHost: String = System.getenv()["DB_HOST"]!!
    public val dbPort: String = System.getenv()["DB_PORT"]!!
    public val dbSchema: String = System.getenv()["DB_SCHEMA"]!!
    public val dbUserName: String = System.getenv()["DB_USERNAME"]!!
    public val dbPassword: String = System.getenv()["DB_PASSWORD"]!!

    public val adminPassword: String = System.getenv()["ADMIN_PASSWORD"]!!

    public val userPasswordPepper: String = System.getenv("USER_PASSWORD_PEPPER")!!

    public val enableRedis: Boolean = System.getenv("ENABLE_REDIS")?.toBooleanStrictOrNull() ?: false
    public val redisHost: String? = System.getenv("REDIS_HOST").takeIf { it.isNotBlank() }
    public val redisPort: Int? = System.getenv("REDIS_PORT")?.toInt()
}
