package net.matsudamper.money.categoryfilter.matchexpression

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class MatchExpressionAnalyzerTest : DescribeSpec(
    {
        val matcherKeys = setOf("id1", "id2", "id3", "a_b-c")

        fun evaluate(source: String, trueKeys: Set<String>): Boolean {
            val analysis = MatchExpressionAnalyzer.analyze(source, matcherKeys)
            analysis.shouldBeInstanceOf<MatchExpressionAnalysis.Valid>()
            return analysis.expression.evaluate { it in trueKeys }
        }

        fun errors(source: String): List<MatchExpressionError> {
            val analysis = MatchExpressionAnalyzer.analyze(source, matcherKeys)
            analysis.shouldBeInstanceOf<MatchExpressionAnalysis.Invalid>()
            return analysis.errors
        }

        describe("評価") {
            it("ANDはORより優先される") {
                evaluate("id1 OR id2 AND id3", trueKeys = setOf("id1")).shouldBe(true)
                evaluate("id1 AND id2 OR id3", trueKeys = setOf("id3")).shouldBe(true)
                evaluate("id1 AND id2 OR id3", trueKeys = setOf("id1")).shouldBe(false)
            }

            it("括弧で優先順位を変えられる") {
                evaluate("(id1 OR id2) AND id3", trueKeys = setOf("id1")).shouldBe(false)
                evaluate("(id1 OR id2) AND id3", trueKeys = setOf("id1", "id3")).shouldBe(true)
            }

            it("!はANDより優先される") {
                evaluate("!id1 AND id2", trueKeys = setOf("id2")).shouldBe(true)
                evaluate("!id1 AND id2", trueKeys = setOf("id1", "id2")).shouldBe(false)
                evaluate("!!id1", trueKeys = setOf("id1")).shouldBe(true)
                evaluate("!(id1 OR id2)", trueKeys = setOf()).shouldBe(true)
            }

            it("TRUEとFALSEを使える") {
                evaluate("TRUE AND !FALSE", trueKeys = setOf()).shouldBe(true)
                evaluate("FALSE OR id1", trueKeys = setOf()).shouldBe(false)
            }

            it("改行とタブは空白として扱う") {
                evaluate("id1\nAND\r\n\tid2", trueKeys = setOf("id1", "id2")).shouldBe(true)
            }

            it("matcher_keyに_と-を使える") {
                evaluate("a_b-c", trueKeys = setOf("a_b-c")).shouldBe(true)
            }

            it("空白なしで括弧と!を書ける") {
                evaluate("!(id1)AND(id2)", trueKeys = setOf("id2")).shouldBe(true)
            }
        }

        describe("エラー") {
            it("空の式はエラー") {
                errors("  ").map { it.type }.shouldBe(listOf(MatchExpressionError.Type.Empty))
            }

            it("使えない文字は連続した範囲をまとめてエラーにする") {
                errors("id1 && id2").shouldBe(
                    listOf(MatchExpressionError(MatchExpressionError.Type.InvalidCharacter, 4..5, "&&")),
                )
            }

            it("全角空白は使えない文字として扱う") {
                errors("id1　AND id2").map { it.type }.shouldBe(listOf(MatchExpressionError.Type.InvalidCharacter))
            }

            it("知らない大文字語はエラー") {
                errors("id1 And id2").shouldBe(
                    listOf(MatchExpressionError(MatchExpressionError.Type.UnknownKeyword, 4..6, "And")),
                )
                errors("id1 XOR id2").map { it.type }.shouldBe(listOf(MatchExpressionError.Type.UnknownKeyword))
            }

            it("存在しないmatcher_keyはエラー") {
                errors("id1 OR id9 OR id8").shouldBe(
                    listOf(
                        MatchExpressionError(MatchExpressionError.Type.UnknownMatcherKey, 7..9, "id9"),
                        MatchExpressionError(MatchExpressionError.Type.UnknownMatcherKey, 14..16, "id8"),
                    ),
                )
            }

            it("演算子の後に式が無い場合はエラー") {
                errors("id1 AND").shouldBe(
                    listOf(MatchExpressionError(MatchExpressionError.Type.MissingOperand, 4..6, "AND")),
                )
            }

            it("閉じ括弧が無い場合は開き括弧の位置をエラーにする") {
                errors("(id1 OR id2").shouldBe(
                    listOf(MatchExpressionError(MatchExpressionError.Type.MissingClosingParenthesis, 0..0, "(")),
                )
            }

            it("余分なトークンはエラー") {
                errors("id1 id2").shouldBe(
                    listOf(MatchExpressionError(MatchExpressionError.Type.UnexpectedToken, 4..6, "id2")),
                )
                errors("id1)").shouldBe(
                    listOf(MatchExpressionError(MatchExpressionError.Type.UnexpectedToken, 3..3, ")")),
                )
                errors("AND id1").shouldBe(
                    listOf(MatchExpressionError(MatchExpressionError.Type.UnexpectedToken, 0..2, "AND")),
                )
            }

            it("構文エラーと存在しないmatcher_keyのエラーを両方返す") {
                errors("id9 AND").map { it.type }.shouldBe(
                    listOf(
                        MatchExpressionError.Type.UnknownMatcherKey,
                        MatchExpressionError.Type.MissingOperand,
                    ),
                )
            }
        }
    },
)
