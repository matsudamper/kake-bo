package net.matsudamper.money.backend.lib

import net.matsudamper.money.backend.base.lib.decodeBase64String
import net.matsudamper.money.backend.base.lib.encodeBase64

internal object CursorParser {
    fun createToString(keyValues: Map<String, String>): String {
        return keyValues.map { (key, value) ->
            val key64 = key.encodeBase64().replace("=", "")
            val value64 = value.encodeBase64().replace("=", "")
            "$key64=$value64"
        }.joinToString("&")
    }

    fun parseFromString(cursorString: String): Map<String, String> {
        return cursorString.split("&").associate {
            val (key, value) = it.split("=")
            key.decodeBase64String() to value.decodeBase64String()
        }
    }
}
