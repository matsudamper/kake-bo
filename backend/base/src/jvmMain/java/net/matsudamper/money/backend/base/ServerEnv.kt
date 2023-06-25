package net.matsudamper.money.backend.base

object ServerEnv {
    val isSecure: Boolean = System.getenv()["IS_SECURE"].toBoolean()
    val domain: String = System.getenv()["DOMAIN"]!!
    val port: Int = System.getenv()["PORT"]!!.toInt()
    val frontPath = System.getenv()["HTML_PATH"] ?: "./frontend/jsApp/build/developmentExecutable"
    val htmlPath = "$frontPath/index.html"

    val dbHost: String = System.getenv()["DB_HOST"]!!
    val dbSchema: String = System.getenv()["DB_SCHEMA"]!!
    val dbUserName: String = System.getenv()["DB_USERNAME"]!!
    val dbPassword: String = System.getenv()["DB_PASSWORD"]!!

    val adminPassword = System.getenv()["ADMIN_PASSWORD"]!!

    val userPasswordPepper: String = System.getenv("USER_PASSWORD_PEPPER")!!
}
