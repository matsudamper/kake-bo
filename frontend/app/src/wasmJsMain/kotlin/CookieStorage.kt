import kotlinx.browser.document

internal object CookieStorage {
    operator fun get(key: String): String? {
        return document.cookie.split(";")
            .asSequence()
            .map {
                val result = it.split("=")

                result[0] to result[1]
            }.firstOrNull {
                it.first == key
            }?.second
    }
}
