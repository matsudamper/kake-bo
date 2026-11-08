package net.matsudamper.money.backend.graphql

import graphql.ExecutionResult
import graphql.GraphQL
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class SingleExecutionPerRequestInstrumentationTest : DescribeSpec({
    describe("SingleExecutionPerRequestInstrumentation") {
        val sdl = """
            directive @singleExecutionPerRequest on FIELD_DEFINITION

            type Query {
                login: String! @singleExecutionPerRequest
                fidoLogin: String! @singleExecutionPerRequest
                normal: String!
                nested: Nested!
            }

            type Nested {
                login: String! @singleExecutionPerRequest
            }
        """.trimIndent()

        var executedCount = 0
        fun execute(query: String): ExecutionResult {
            executedCount = 0
            val wiring = RuntimeWiring.newRuntimeWiring()
                .type("Query") { builder ->
                    builder
                        .dataFetcher("login") {
                            executedCount++
                            "ok"
                        }
                        .dataFetcher("fidoLogin") {
                            executedCount++
                            "ok"
                        }
                        .dataFetcher("normal") {
                            executedCount++
                            "ok"
                        }
                        .dataFetcher("nested") { Any() }
                }
                .type("Nested") { builder ->
                    builder.dataFetcher("login") {
                        executedCount++
                        "ok"
                    }
                }
                .build()
            val schema = SchemaGenerator().makeExecutableSchema(SchemaParser().parse(sdl), wiring)
            val graphql = GraphQL.newGraphQL(schema)
                .instrumentation(SingleExecutionPerRequestInstrumentation())
                .build()
            return graphql.execute(query)
        }

        it("対象フィールドが1つなら実行される") {
            val result = execute("query { login }")

            result.errors.shouldBeEmpty()
            executedCount shouldBe 1
        }

        it("対象外のフィールドは複数あっても実行される") {
            val result = execute("query { normal a: normal b: normal }")

            result.errors.shouldBeEmpty()
            executedCount shouldBe 3
        }

        it("対象フィールドと対象外フィールドの組み合わせは実行される") {
            val result = execute("query { login normal }")

            result.errors.shouldBeEmpty()
            executedCount shouldBe 2
        }

        it("エイリアスで同じ対象フィールドを複数並べるとresolver実行前に中断する") {
            val result = execute("query { a: login b: login }")

            result.errors shouldHaveSize 1
            result.errors.first().message shouldContain "singleExecutionPerRequest"
            executedCount shouldBe 0
        }

        it("異なる対象フィールドを並べても中断する") {
            val result = execute("query { login fidoLogin }")

            result.errors shouldHaveSize 1
            executedCount shouldBe 0
        }

        it("ネストした対象フィールドも数える") {
            val result = execute("query { login nested { login } }")

            result.errors shouldHaveSize 1
            executedCount shouldBe 0
        }

        it("fragment経由で複数展開しても中断する") {
            val result = execute(
                """
                query {
                    a: nested { ...F }
                    b: nested { ...F }
                }
                fragment F on Nested { login }
                """.trimIndent(),
            )

            result.errors shouldHaveSize 1
            executedCount shouldBe 0
        }
    }
})
