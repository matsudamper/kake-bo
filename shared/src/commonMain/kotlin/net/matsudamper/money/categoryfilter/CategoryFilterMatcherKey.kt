package net.matsudamper.money.categoryfilter

object CategoryFilterMatcherKey {
    const val MAX_LENGTH: Int = 50
    const val ALLOWED_CHARACTERS_DESCRIPTION: String = "半角小文字の英字・数字・_・-"

    fun isValid(key: String): Boolean {
        return key.isNotEmpty() && key.length <= MAX_LENGTH && key.all { isAllowedCharacter(it) }
    }

    fun isAllowedCharacter(char: Char): Boolean {
        return char in 'a'..'z' || char in '0'..'9' || char == '_' || char == '-'
    }
}
