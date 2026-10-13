package net.matsudamper.money.categoryfilter.matchexpression

object MatchExpressionAnalyzer {
    const val MAX_LENGTH: Int = 5000

    fun analyze(source: String, matcherKeys: Set<String>): MatchExpressionAnalysis {
        if (source.length > MAX_LENGTH) {
            val overflowRange = MAX_LENGTH until source.length
            return MatchExpressionAnalysis.Invalid(
                errors = listOf(
                    MatchExpressionError(
                        type = MatchExpressionError.Type.TooLong,
                        range = overflowRange,
                        text = source.substring(overflowRange),
                    ),
                ),
            )
        }
        val lexResult = MatchExpressionLexer.tokenize(source)
        val unknownMatcherKeyErrors = lexResult.tokens
            .filterIsInstance<MatchExpressionToken.MatcherKey>()
            .filter { it.key !in matcherKeys }
            .map { token ->
                MatchExpressionError(
                    type = MatchExpressionError.Type.UnknownMatcherKey,
                    range = token.range,
                    text = token.key,
                )
            }
        // 字句エラーがあるとトークン列が欠けて的外れな構文エラーになるため、構文解析は字句エラーが無いときだけ行う
        if (lexResult.errors.isNotEmpty()) {
            return MatchExpressionAnalysis.Invalid(
                errors = (lexResult.errors + unknownMatcherKeyErrors).sortedBy { it.range.first },
            )
        }
        return when (val parseResult = MatchExpressionParser.parse(source = source, tokens = lexResult.tokens)) {
            is MatchExpressionParseResult.Failure -> MatchExpressionAnalysis.Invalid(
                errors = (unknownMatcherKeyErrors + parseResult.error).sortedBy { it.range.first },
            )

            is MatchExpressionParseResult.Success -> {
                if (unknownMatcherKeyErrors.isEmpty()) {
                    MatchExpressionAnalysis.Valid(parseResult.expression)
                } else {
                    MatchExpressionAnalysis.Invalid(errors = unknownMatcherKeyErrors)
                }
            }
        }
    }
}

sealed interface MatchExpressionAnalysis {
    data class Valid(val expression: MatchExpression) : MatchExpressionAnalysis

    data class Invalid(val errors: List<MatchExpressionError>) : MatchExpressionAnalysis
}
