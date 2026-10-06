package net.matsudamper.money.categoryfilter.matchexpression

import net.matsudamper.money.categoryfilter.CategoryFilterMatcherKey

object MatchExpressionLexer {
    fun tokenize(source: String): MatchExpressionLexResult {
        val lexemes = buildList {
            var index = 0
            while (index < source.length) {
                val char = source[index]
                val lexeme = when {
                    isWhitespace(char) -> null
                    char == '(' -> Lexeme.Token(MatchExpressionToken.LeftParenthesis(index..index))
                    char == ')' -> Lexeme.Token(MatchExpressionToken.RightParenthesis(index..index))
                    char == '!' -> Lexeme.Token(MatchExpressionToken.Not(index..index))
                    isWordCharacter(char) -> readWord(source = source, start = index)
                    else -> readInvalidCharacters(source = source, start = index)
                }
                if (lexeme != null) {
                    add(lexeme)
                    index = lexeme.range.last + 1
                } else {
                    index++
                }
            }
        }
        return MatchExpressionLexResult(
            tokens = lexemes.filterIsInstance<Lexeme.Token>().map { it.token },
            errors = lexemes.filterIsInstance<Lexeme.Error>().map { it.error },
        )
    }

    private fun readWord(source: String, start: Int): Lexeme {
        val end = findEnd(source = source, start = start) { isWordCharacter(it) }
        val range = start until end
        val word = source.substring(range)
        return when {
            word == "AND" -> Lexeme.Token(MatchExpressionToken.And(range))
            word == "OR" -> Lexeme.Token(MatchExpressionToken.Or(range))
            word == "TRUE" -> Lexeme.Token(MatchExpressionToken.Literal(value = true, range = range))
            word == "FALSE" -> Lexeme.Token(MatchExpressionToken.Literal(value = false, range = range))
            word.all { CategoryFilterMatcherKey.isAllowedCharacter(it) } -> {
                Lexeme.Token(MatchExpressionToken.MatcherKey(key = word, range = range))
            }

            else -> Lexeme.Error(
                MatchExpressionError(
                    type = MatchExpressionError.Type.UnknownKeyword,
                    range = range,
                    text = word,
                ),
            )
        }
    }

    private fun readInvalidCharacters(source: String, start: Int): Lexeme {
        val end = findEnd(source = source, start = start) { isInvalidCharacter(it) }
        val range = start until end
        return Lexeme.Error(
            MatchExpressionError(
                type = MatchExpressionError.Type.InvalidCharacter,
                range = range,
                text = source.substring(range),
            ),
        )
    }

    private fun findEnd(source: String, start: Int, predicate: (Char) -> Boolean): Int {
        return (start until source.length)
            .firstOrNull { !predicate(source[it]) }
            ?: source.length
    }

    private fun isWhitespace(char: Char): Boolean {
        return char == ' ' || char == '\t' || char == '\n' || char == '\r'
    }

    private fun isWordCharacter(char: Char): Boolean {
        return CategoryFilterMatcherKey.isAllowedCharacter(char) || char in 'A'..'Z'
    }

    private fun isInvalidCharacter(char: Char): Boolean {
        return !isWhitespace(char) && !isWordCharacter(char) && char != '(' && char != ')' && char != '!'
    }

    private sealed interface Lexeme {
        val range: IntRange

        data class Token(val token: MatchExpressionToken) : Lexeme {
            override val range: IntRange = token.range
        }

        data class Error(val error: MatchExpressionError) : Lexeme {
            override val range: IntRange = error.range
        }
    }
}

data class MatchExpressionLexResult(
    val tokens: List<MatchExpressionToken>,
    val errors: List<MatchExpressionError>,
)
