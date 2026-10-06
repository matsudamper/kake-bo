package net.matsudamper.money.detekt

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize

class KtorRuntimeSerializerLookupTest : FunSpec(
    {
        val rule = KtorRuntimeSerializerLookup(Config.empty)

        test("call.respond を検知する") {
            val code = """
                import io.ktor.server.response.respond

                suspend fun handle(call: Any) {
                    call.respond(Body())
                }
            """.trimIndent()
            rule.lint(code, compile = false).shouldHaveSize(1)
        }

        test("respondNullable を検知する") {
            val code = """
                import io.ktor.server.response.respondNullable

                suspend fun handle(call: Any) {
                    call.respondNullable(Body())
                }
            """.trimIndent()
            rule.lint(code, compile = false).shouldHaveSize(1)
        }

        test("star import の receive を検知する") {
            val code = """
                import io.ktor.server.request.*

                suspend fun handle(call: Any) {
                    val body = call.receive<Body>()
                }
            """.trimIndent()
            rule.lint(code, compile = false).shouldHaveSize(1)
        }

        test("型引数付きの post を検知する") {
            val code = """
                import io.ktor.server.routing.post

                fun route() {
                    post<Body>("/path") { body -> }
                }
            """.trimIndent()
            rule.lint(code, compile = false).shouldHaveSize(1)
        }

        test("ラムダ引数の型から推論される post を検知する") {
            val code = """
                import io.ktor.server.routing.post

                fun route() {
                    post("/path") { body: Body -> }
                }
            """.trimIndent()
            rule.lint(code, compile = false).shouldHaveSize(1)
        }

        test("値引数として渡した型付きラムダの post を検知する") {
            val code = """
                import io.ktor.server.routing.post

                fun route() {
                    post("/path", { body: Body -> })
                }
            """.trimIndent()
            rule.lint(code, compile = false).shouldHaveSize(1)
        }

        test("関数参照を渡した post を検知する") {
            val code = """
                import io.ktor.server.routing.post

                fun route() {
                    post("/path", ::handle)
                }
            """.trimIndent()
            rule.lint(code, compile = false).shouldHaveSize(1)
        }

        test("エイリアス付き import の respond を検知する") {
            val code = """
                import io.ktor.server.response.respond as ktorRespond

                suspend fun handle(call: Any) {
                    call.ktorRespond(Body())
                }
            """.trimIndent()
            rule.lint(code, compile = false).shouldHaveSize(1)
        }

        test("型引数なしの post と respondText は検知しない") {
            val code = """
                import io.ktor.server.response.respondText
                import io.ktor.server.routing.post

                fun route() {
                    post("/path") {
                        call.respondText("ok")
                    }
                }
            """.trimIndent()
            rule.lint(code, compile = false).shouldBeEmpty()
        }

        test("名前付き引数で body を先に渡した型なしの post は検知しない") {
            val code = """
                import io.ktor.server.routing.post

                fun route() {
                    post(body = { call.respondText("ok") }, path = "/path")
                }
            """.trimIndent()
            rule.lint(code, compile = false).shouldBeEmpty()
        }

        test("HttpStatusCode だけを渡す respond は検知しない") {
            val code = """
                import io.ktor.server.response.respond

                suspend fun handle(call: Any) {
                    call.respond(HttpStatusCode.NoContent)
                }
            """.trimIndent()
            rule.lint(code, compile = false).shouldBeEmpty()
        }

        test("HttpStatusCode と本文を渡す respond を検知する") {
            val code = """
                import io.ktor.server.response.respond

                suspend fun handle(call: Any) {
                    call.respond(HttpStatusCode.OK, Body())
                }
            """.trimIndent()
            rule.lint(code, compile = false).shouldHaveSize(1)
        }

        test("クライアントの post と同時に import したサーバーの型付き post を検知する") {
            val code = """
                import io.ktor.client.request.post
                import io.ktor.server.routing.post

                fun route() {
                    post<Body>("/path") { body -> }
                }
            """.trimIndent()
            rule.lint(code, compile = false).shouldHaveSize(1)
        }

        test("同名の明示 import があっても star import の Ktor respond を検知する") {
            val code = """
                import example.respond
                import io.ktor.server.response.*

                suspend fun handle(call: Any) {
                    call.respond(Body())
                }
            """.trimIndent()
            rule.lint(code, compile = false).shouldHaveSize(1)
        }

        test("HttpStatusCode 配下のリストを渡す respond を検知する") {
            val code = """
                import io.ktor.server.response.respond

                suspend fun handle(call: Any) {
                    call.respond(HttpStatusCode.allStatusCodes)
                }
            """.trimIndent()
            rule.lint(code, compile = false).shouldHaveSize(1)
        }

        test("型引数付きの query を検知する") {
            val code = """
                import io.ktor.server.routing.query

                fun route() {
                    query<Body>("/path") { body -> }
                }
            """.trimIndent()
            rule.lint(code, compile = false).shouldHaveSize(1)
        }

        test("Ktor 以外の同名関数は検知しない") {
            val code = """
                import example.respond

                fun handle() {
                    respond(Body())
                }
            """.trimIndent()
            rule.lint(code, compile = false).shouldBeEmpty()
        }
    },
)
