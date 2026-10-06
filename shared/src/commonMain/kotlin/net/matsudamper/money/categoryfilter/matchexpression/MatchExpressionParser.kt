package net.matsudamper.money.categoryfilter.matchexpression

/**
 * 優先順位は `!` > `AND` > `OR`。二項演算子は左結合
 */
object MatchExpressionParser {
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

        fun readAll(): MatchExpression {
            val expression = readOr()
            val remaining = tokens.getOrNull(position)
            if (remaining != null) {
                throw ParseException(createError(MatchExpressionError.Type.UnexpectedToken, remaining.range))
            }
            return expression
        }

        private fun readOr(): MatchExpression {
            var expression = readAnd()
            while (tokens.getOrNull(position) is MatchExpressionToken.Or) {
                position++
                expression = MatchExpression.Or(left = expression, right = readAnd())
            }
            return expression
        }

        private fun readAnd(): MatchExpression {
            var expression = readUnary()
            while (tokens.getOrNull(position) is MatchExpressionToken.And) {
                position++
                expression = MatchExpression.And(left = expression, right = readUnary())
            }
            return expression
        }

        private fun readUnary(): MatchExpression {
            return if (tokens.getOrNull(position) is MatchExpressionToken.Not) {
                position++
                MatchExpression.Not(readUnary())
            } else {
                readPrimary()
            }
        }

        private fun readPrimary(): MatchExpression {
            val token = tokens.getOrNull(position)
                ?: throw ParseException(createError(MatchExpressionError.Type.MissingOperand, tokens.last().range))
            position++
            return when (token) {
                is MatchExpressionToken.MatcherKey -> MatchExpression.MatcherReference(token.key)
                is MatchExpressionToken.Literal -> MatchExpression.Literal(token.value)
                is MatchExpressionToken.LeftParenthesis -> {
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
