package net.matsudamper.money.backend.mail.parser.lib

internal object ParseUtil {
    fun getInt(value: String): Int? {
        return value
            .mapNotNull { it.toString().toIntOrNull() }
            .joinToString("")
            .toIntOrNull()
    }

    fun removeHtmlTag(value: String): String {
        return "<.+?>".toRegex().replace(value, "")
    }

    fun splitByNewLine(value: String): List<String> {
        return value.split("\r\n")
            .flatMap { it.split("\n") }
    }
}
