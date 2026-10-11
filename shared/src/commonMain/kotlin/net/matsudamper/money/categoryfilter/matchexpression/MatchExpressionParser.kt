package net.matsudamper.money.categoryfilter.matchexpression

/**
 * 優先順位は `!` > `AND` > `OR`。二項演算子は左結合
 */
object MatchExpressionParser {
    /** 括弧と `!` の入れ子の上限。解析と評価が再帰するため、スタックを使い切らないように制限する */
    const val MAX_NESTING_DEPTH: Int = 100

    fun parse(source: String, tokens: List<MatchExpressionToken>): MatchExpressionParseResult {
        if (tokens.isEmpty()) {
            return MatchExpressionParseResult.Failure(
                MatchExpressionError(
                    type = MatchExpressionError.Type.Empty,
                    range = source.indices,
                    text = source,
                ),
            )
        }
        return try {
            MatchExpressionParseResult.Success(
                TokenReader(source = source, tokens = tokens).readAll(),
            )
        } catch (e: ParseException) {
            MatchExpressionParseResult.Failure(e.error)
        }
    }

    private class TokenReader(
        private val source: String,
        private val tokens: List<MatchExpressionToken>,
    ) {
        private var position = 0
        private var nestingDepth = 0

        fun readAll(): MatchExpression {
            val expression = readOr()
            val remaining = tokens.getOrNull(position)
            if (remaining != null) {
                throw ParseException(createError(MatchExpressionError.Type.UnexpectedToken, remaining.range))
            }
            return expression
        }

        private fun readOr(): MatchExpression {
            val operands = mutableListOf(readAnd())
            while (tokens.getOrNull(position) is MatchExpressionToken.Or) {
                position++
                operands.add(readAnd())
            }
            return operands.singleOrNull() ?: MatchExpression.Or(operands)
        }

        private fun readAnd(): MatchExpression {
            val operands = mutableListOf(readUnary())
            while (tokens.getOrNull(position) is MatchExpressionToken.And) {
                position++
                operands.add(readUnary())
            }
            return operands.singleOrNull() ?: MatchExpression.And(operands)
        }

        private fun readUnary(): MatchExpression {
            val token = tokens.getOrNull(position)
            return if (token is MatchExpressionToken.Not) {
                position++
                nested(token) { MatchExpression.Not(readUnary()) }
            } else {
                readPrimary()
            }
        }

        private fun <T> nested(token: MatchExpressionToken, block: () -> T): T {
            nestingDepth++
            if (nestingDepth > MAX_NESTING_DEPTH) {
                throw ParseException(createError(MatchExpressionError.Type.TooDeepNesting, token.range))
            }
            return block().also { nestingDepth-- }
        }

        private fun readPrimary(): MatchExpression {
            val token = tokens.getOrNull(position)
                ?: throw ParseException(createError(MatchExpressionError.Type.MissingOperand, tokens.last().range))
            position++
            return when (token) {
                is MatchExpressionToken.MatcherKey -> MatchExpression.MatcherReference(token.key)
                is MatchExpressionToken.Literal -> MatchExpression.Literal(token.value)
                is MatchExpressionToken.LeftParenthesis -> nested(token) {
                    val expression = readOr()
                    if (tokens.getOrNull(position) !is MatchExpressionToken.RightParenthesis) {
                        throw ParseException(createError(MatchExpressionError.Type.MissingClosingParenthesis, token.range))
                    }
                    position++
                    expression
                }

                is MatchExpressionToken.And,
                is MatchExpressionToken.Or,
                is MatchExpressionToken.Not,
                is MatchExpressionToken.RightParenthesis,
                -> throw ParseException(createError(MatchExpressionError.Type.UnexpectedToken, token.range))
            }
        }

        private fun createError(type: MatchExpressionError.Type, range: IntRange): MatchExpressionError {
            return MatchExpressionError(
                type = type,
                range = range,
                text = source.substring(range),
            )
        }
    }

    private class ParseException(val error: MatchExpressionError) : Exception()
}

sealed interface MatchExpressionParseResult {
    data class Success(val expression: MatchExpression) : MatchExpressionParseResult

    data class Failure(val error: MatchExpressionError) : MatchExpressionParseResult
}
