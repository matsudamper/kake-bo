package net.matsudamper.money.backend.logic

object ColorValidator {
    private val colorRegex = Regex("^#[0-9A-Fa-f]{6}$")

    fun isValid(color: String?): Boolean {
        if (color == null) return true
        return colorRegex.matches(color)
    }
}
