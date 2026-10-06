package net.matsudamper.money.categoryfilter.matchexpression

/**
 * @param range エラー箇所の文字範囲
 * @param text エラー箇所の文字列
 */
data class MatchExpressionError(
    val type: Type,
    val range: IntRange,
    val text: String,
) {
    enum class Type {
        Empty,
        InvalidCharacter,
        UnknownKeyword,
        UnexpectedToken,
        MissingOperand,
        MissingClosingParenthesis,
        UnknownMatcherKey,
    }
}
