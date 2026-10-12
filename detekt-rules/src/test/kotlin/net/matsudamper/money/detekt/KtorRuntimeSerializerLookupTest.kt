package net.matsudamper.money.detekt

import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize

class KtorRuntimeSerializerLookupTest : FunSpec(
    {
        val environment = createEnvironment()
        val rule = KtorRuntimeSerializerLookup(Config.empty)

        fun lint(code: String) = rule.lintWithContext(environment, code, *dependencyStubs)

        test("call.respond を検知する") {
            val code = """
                import io.ktor.server.application.ApplicationCall
                import io.ktor.server.response.respond

                class Body
                suspend fun handle(call: ApplicationCall) {
                    call.respond(Body())
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("respondNullable を検知する") {
            val code = """
                import io.ktor.server.application.ApplicationCall
                import io.ktor.server.response.respondNullable

                class Body
                suspend fun handle(call: ApplicationCall) {
                    call.respondNullable(Body())
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("star import の receive を検知する") {
            val code = """
                import io.ktor.server.application.ApplicationCall
                import io.ktor.server.request.*

                class Body
                suspend fun handle(call: ApplicationCall) {
                    val body = call.receive<Body>()
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("型引数付きの post を検知する") {
            val code = """
                import io.ktor.server.routing.Route
                import io.ktor.server.routing.post

                class Body
                fun route(route: Route) {
                    route.post<Body>("/path") { body -> }
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("ラムダ引数の型から推論される post を検知する") {
            val code = """
                import io.ktor.server.routing.Route
                import io.ktor.server.routing.post

                class Body
                fun route(route: Route) {
                    route.post("/path") { body: Body -> }
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("値引数として渡した型付きラムダの post を検知する") {
            val code = """
                import io.ktor.server.routing.Route
                import io.ktor.server.routing.RoutingContext
                import io.ktor.server.routing.post

                class Body
                fun route(route: Route) {
                    val handler: RoutingContext.(Body) -> Unit = { body -> }
                    route.post("/path", handler)
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("関数参照を渡した型付き post を検知する") {
            val code = """
                import io.ktor.server.routing.Route
                import io.ktor.server.routing.RoutingContext
                import io.ktor.server.routing.post

                class Body
                fun handle(context: RoutingContext, body: Body) {}
                fun route(route: Route) {
                    route.post("/path", ::handle)
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("エイリアス付き import の respond を検知する") {
            val code = """
                import io.ktor.server.application.ApplicationCall
                import io.ktor.server.response.respond as ktorRespond

                class Body
                suspend fun handle(call: ApplicationCall) {
                    call.ktorRespond(Body())
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("型引数なしの post と respondText は検知しない") {
            val code = """
                import io.ktor.server.application.ApplicationCall
                import io.ktor.server.response.respondText
                import io.ktor.server.routing.Route
                import io.ktor.server.routing.post

                fun route(route: Route, call: ApplicationCall) {
                    route.post("/path") {
                        call.respondText("ok")
                    }
                }
            """.trimIndent()
            lint(code).shouldBeEmpty()
        }

        test("名前付き引数で body を先に渡した型なしの post は検知しない") {
            val code = """
                import io.ktor.server.routing.Route
                import io.ktor.server.routing.post

                fun route(route: Route) {
                    route.post(body = { }, path = "/path")
                }
            """.trimIndent()
            lint(code).shouldBeEmpty()
        }

        test("HttpStatusCode だけを渡す respond は検知しない") {
            val code = """
                import io.ktor.http.HttpStatusCode
                import io.ktor.server.application.ApplicationCall
                import io.ktor.server.response.respond

                suspend fun handle(call: ApplicationCall) {
                    call.respond(HttpStatusCode.NoContent)
                }
            """.trimIndent()
            lint(code).shouldBeEmpty()
        }

        test("HttpStatusCode と本文を渡す respond を検知する") {
            val code = """
                import io.ktor.http.HttpStatusCode
                import io.ktor.server.application.ApplicationCall
                import io.ktor.server.response.respond

                class Body
                suspend fun handle(call: ApplicationCall) {
                    call.respond(HttpStatusCode.OK, Body())
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("クライアントの post と同時に import したサーバーの型付き post を検知する") {
            val code = """
                import io.ktor.client.request.post
                import io.ktor.server.routing.Route
                import io.ktor.server.routing.post

                class Body
                fun route(route: Route) {
                    route.post<Body>("/path") { body -> }
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("同名の明示 import があってもレシーバ付きの Ktor respond を検知する") {
            val code = """
                import example.respond
                import io.ktor.server.application.ApplicationCall
                import io.ktor.server.response.respond

                class Body
                suspend fun handle(call: ApplicationCall) {
                    call.respond(Body())
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("HttpStatusCode 配下のリストを渡す respond を検知する") {
            val code = """
                import io.ktor.http.HttpStatusCode
                import io.ktor.server.application.ApplicationCall
                import io.ktor.server.response.respond

                suspend fun handle(call: ApplicationCall) {
                    call.respond(HttpStatusCode.allStatusCodes)
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("型引数付きの query を検知する") {
            val code = """
                import io.ktor.server.routing.Route
                import io.ktor.server.routing.query

                class Body
                fun route(route: Route) {
                    route.query<Body>("/path") { body -> }
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("型付き overload のない get は検知しない") {
            val code = """
                import io.ktor.server.routing.get

                class Service
                class Context {
                    fun <T> get(): T = error("stub")
                }
                fun route(context: Context) {
                    val service = context.get<Service>()
                }
            """.trimIndent()
            lint(code).shouldBeEmpty()
        }

        test("Ktor 以外の同名関数は検知しない") {
            val code = """
                import example.respond

                class Body
                fun handle() {
                    respond(Body())
                }
            """.trimIndent()
            lint(code).shouldBeEmpty()
        }

        test("ObjectMapper.kotlinxSerialization.encodeToString を検知する") {
            val code = """
                import net.matsudamper.money.backend.base.ObjectMapper

                class Body
                fun save(data: Body) {
                    ObjectMapper.kotlinxSerialization.encodeToString(data)
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("serializer を渡した encodeToString は検知しない") {
            val code = """
                import kotlinx.serialization.SerializationStrategy
                import net.matsudamper.money.backend.base.ObjectMapper

                class Body {
                    companion object {
                        fun serializer(): SerializationStrategy<Body> = error("stub")
                    }
                }
                fun save(data: Body) {
                    ObjectMapper.kotlinxSerialization.encodeToString(Body.serializer(), data)
                }
            """.trimIndent()
            lint(code).shouldBeEmpty()
        }

        test("reified な decodeFromString を検知する") {
            val code = """
                import kotlinx.serialization.json.Json

                class Body
                fun load(json: Json, text: String) {
                    json.decodeFromString<Body>(text)
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("名前をルールに持たない新しい format API を検知する") {
            val code = """
                import kotlinx.serialization.json.Json

                class Body
                fun save(json: Json, data: Body) {
                    json.encodeToFutureFormat(data)
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("StringFormat の新しい reified 拡張を検知する") {
            val code = """
                import kotlinx.serialization.StringFormat
                import kotlinx.serialization.encodeToFuture

                class Body
                fun save(format: StringFormat, data: Body) {
                    format.encodeToFuture(data)
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("Ktor パッケージの新しい reified API を検知する") {
            val code = """
                import io.ktor.server.application.ApplicationCall
                import io.ktor.server.response.respondUpcoming

                class Body
                suspend fun handle(call: ApplicationCall) {
                    call.respondUpcoming(Body())
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("KSerializer を受け取る reified API は検知しない") {
            val code = """
                import kotlinx.serialization.KSerializer
                import kotlinx.serialization.json.Json

                class Body
                fun save(json: Json, serializer: KSerializer<Body>, data: Body) {
                    json.encodeWithSerializer(serializer, data)
                }
            """.trimIndent()
            lint(code).shouldBeEmpty()
        }

        test("Base64.encodeToString は検知しない") {
            val code = """
                import java.util.Base64

                fun encode() {
                    Base64.getEncoder().encodeToString(byteArrayOf())
                }
            """.trimIndent()
            lint(code).shouldBeEmpty()
        }

        test("reified な serializer 関数を検知する") {
            val code = """
                import kotlinx.serialization.serializer

                class Body
                fun lookup() {
                    serializer<Body>()
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }
    },
)

private val dependencyStubs = arrayOf(
    """
        package io.ktor.http

        class HttpStatusCode {
            companion object {
                val NoContent: HttpStatusCode = HttpStatusCode()
                val OK: HttpStatusCode = HttpStatusCode()
                val allStatusCodes: List<HttpStatusCode> = listOf()
            }
        }
    """.trimIndent(),
    """
        package io.ktor.server.application

        class ApplicationCall
    """.trimIndent(),
    """
        package io.ktor.server.response

        import io.ktor.http.HttpStatusCode
        import io.ktor.server.application.ApplicationCall

        suspend inline fun <reified T : Any> ApplicationCall.respond(message: T) {}
        suspend inline fun <reified T> ApplicationCall.respondNullable(message: T) {}
        suspend inline fun <reified T : Any> ApplicationCall.respond(status: HttpStatusCode, message: T) {}
        suspend inline fun <reified T : Any> ApplicationCall.respondUpcoming(message: T) {}
        suspend fun ApplicationCall.respondText(text: String) {}
    """.trimIndent(),
    """
        package io.ktor.server.request

        import io.ktor.server.application.ApplicationCall

        suspend inline fun <reified T : Any> ApplicationCall.receive(): T = error("stub")
        suspend inline fun <reified T> ApplicationCall.receiveNullable(): T? = null
    """.trimIndent(),
    """
        package io.ktor.server.routing

        class Route
        class RoutingContext

        fun Route.post(path: String, body: RoutingContext.() -> Unit): Route = this
        inline fun <reified R : Any> Route.post(path: String, body: RoutingContext.(R) -> Unit): Route = this
        fun Route.post(body: RoutingContext.() -> Unit): Route = this
        inline fun <reified R : Any> Route.query(path: String, body: RoutingContext.(R) -> Unit): Route = this
        fun Route.get(path: String, body: RoutingContext.() -> Unit): Route = this
    """.trimIndent(),
    """
        package io.ktor.client.request

        fun post(path: String) {}
    """.trimIndent(),
    """
        package example

        fun respond(body: Any) {}
    """.trimIndent(),
    """
        package kotlinx.serialization

        interface SerialFormat
        interface StringFormat : SerialFormat
        interface BinaryFormat : SerialFormat
        interface SerializationStrategy<T>
        interface DeserializationStrategy<T>
        interface KSerializer<T> : SerializationStrategy<T>, DeserializationStrategy<T>

        inline fun <reified T> serializer(): KSerializer<T> = error("stub")
        inline fun <reified T> StringFormat.encodeToFuture(value: T): String = ""
    """.trimIndent(),
    """
        package kotlinx.serialization.json

        import kotlinx.serialization.DeserializationStrategy
        import kotlinx.serialization.KSerializer
        import kotlinx.serialization.SerializationStrategy
        import kotlinx.serialization.StringFormat

        class Json : StringFormat {
            inline fun <reified T> encodeToString(value: T): String = ""
            fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String = ""
            inline fun <reified T> decodeFromString(string: String): T = error("stub")
            inline fun <reified T> encodeToFutureFormat(value: T): String = ""
            inline fun <reified T> encodeWithSerializer(serializer: KSerializer<T>, value: T): String = ""
        }
    """.trimIndent(),
    """
        package net.matsudamper.money.backend.base

        import kotlinx.serialization.json.Json

        object ObjectMapper {
            val kotlinxSerialization: Json = Json()
        }
    """.trimIndent(),
)
