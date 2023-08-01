package net.matsudamper.money.backend.lib

import io.ktor.util.decodeBase64String
import io.ktor.util.encodeBase64

object CursorParser {
    fun createToString(keyValues: Map<String, String>): String {
        return keyValues.map { (key, value) ->
            val key64 = key.encodeBase64().replace("=", "")
            val value64 = value.encodeBase64().replace("=", "")
            "$key64=$value64"
        }.joinToString("&")
    }

    fun parseFromString(value: String): Map<String, String> {
        return value.split("&").associate {
            val (key, value) = it.split("=")
            key.decodeBase64String() to value.decodeBase64String()
        }
    }
}
