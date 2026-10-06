package net.matsudamper.money.categoryfilter.matchexpression

sealed interface MatchExpressionToken {
    val range: IntRange

    data class MatcherKey(val key: String, override val range: IntRange) : MatchExpressionToken

    data class Literal(val value: Boolean, override val range: IntRange) : MatchExpressionToken

    data class And(override val range: IntRange) : MatchExpressionToken

    data class Or(override val range: IntRange) : MatchExpressionToken

    data class Not(override val range: IntRange) : MatchExpressionToken

    data class LeftParenthesis(override val range: IntRange) : MatchExpressionToken

    data class RightParenthesis(override val range: IntRange) : MatchExpressionToken
}
