package net.matsudamper.money.categoryfilter.matchexpression

sealed interface MatchExpression {
    fun evaluate(isMatcherMatched: (matcherKey: String) -> Boolean): Boolean

    data class MatcherReference(val matcherKey: String) : MatchExpression {
        override fun evaluate(isMatcherMatched: (matcherKey: String) -> Boolean): Boolean {
            return isMatcherMatched(matcherKey)
        }
    }

    data class Literal(val value: Boolean) : MatchExpression {
        override fun evaluate(isMatcherMatched: (matcherKey: String) -> Boolean): Boolean {
            return value
        }
    }

    data class Not(val operand: MatchExpression) : MatchExpression {
        override fun evaluate(isMatcherMatched: (matcherKey: String) -> Boolean): Boolean {
            return !operand.evaluate(isMatcherMatched)
        }
    }

    data class And(val left: MatchExpression, val right: MatchExpression) : MatchExpression {
        override fun evaluate(isMatcherMatched: (matcherKey: String) -> Boolean): Boolean {
            return left.evaluate(isMatcherMatched) && right.evaluate(isMatcherMatched)
        }
    }

    data class Or(val left: MatchExpression, val right: MatchExpression) : MatchExpression {
        override fun evaluate(isMatcherMatched: (matcherKey: String) -> Boolean): Boolean {
            return left.evaluate(isMatcherMatched) || right.evaluate(isMatcherMatched)
        }
    }
}
