package net.matsudamper.money.backend.lib

import io.ktor.util.decodeBase64String
import io.ktor.util.encodeBase64

object CursorParser {
    fun createToString(keyValues: Map<String, String>): String {
        return keyValues.map { (key, value) ->
            val key64 = key.decodeBase64String().replace("=", "")
            val value64 = value.decodeBase64String().replace("=", "")
            "$key64=$value64"
        }.joinToString("&")
    }

    fun parseFromString(value: String): Map<String, String> {
        return value.decodeBase64String().split("&").associate {
            val (key, value) = it.split("=")
            key.encodeBase64() to value.encodeBase64()
        }
    }
}
