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

    /**
     * 連続した AND を1つにまとめ、長い式でも評価の再帰が深くならないようにしている
     */
    data class And(val operands: List<MatchExpression>) : MatchExpression {
        override fun evaluate(isMatcherMatched: (matcherKey: String) -> Boolean): Boolean {
            return operands.all { it.evaluate(isMatcherMatched) }
        }
    }

    /**
     * 連続した OR を1つにまとめ、長い式でも評価の再帰が深くならないようにしている
     */
    data class Or(val operands: List<MatchExpression>) : MatchExpression {
        override fun evaluate(isMatcherMatched: (matcherKey: String) -> Boolean): Boolean {
            return operands.any { it.evaluate(isMatcherMatched) }
        }
    }
}
