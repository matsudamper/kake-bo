package net.matsudamper.money.backend

import java.io.File
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.request.path
import io.ktor.server.request.receiveStream
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.accept
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import net.matsudamper.money.backend.base.ObjectMapper
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.di.MainDiContainer
import net.matsudamper.money.backend.graphql.MoneyGraphQlSchema
import org.slf4j.event.Level

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty("logback.configurationFile", "logback.xml")

            // Initialize
            MoneyGraphQlSchema.graphql

            val engine =
                embeddedServer(
                    CIO,
                    port = ServerEnv.port,
                    module = Application::myApplicationModule,
                    configure = {
                    },
                )
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    engine.stop(1000, 1000)
                },
            )
            engine.start(wait = true)
        }
    }
}

fun Application.myApplicationModule() {
    install(ForwardedHeaders)
    install(XForwardedHeaders)
    install(Compression)
    install(ContentNegotiation) {
        json(
            json = ObjectMapper.kotlinxSerialization,
            contentType = ContentType.Application.Json,
        )
    }
    install(CORS) {
        allowHost(host = ServerEnv.domain!!, schemes = listOf("https"))
        allowNonSimpleContentTypes = true
    }
    install(CallLogging) {
        level = Level.INFO
        filter { true }

        format { call ->
            buildString {
                appendLine("==========${call.request.path()}==========")
                println("path: ${call.request.path()}")
                appendLine(
                    call.request.headers.entries().joinToString("\n") { (key, value) ->
                        "$key=$value"
                    },
                )
            }
        }
    }

    routing {
        accept(ContentType.Application.Json) {
            post("/query") {
                call.respondText(
                    contentType = ContentType.Application.Json,
                ) {
                    return@respondText withTimeout(5.seconds) {
                        GraphqlHandler(
                            cookieManager = CookieManagerImpl(call = call),
                            diContainer = MainDiContainer(),
                        ).handle(
                            requestText = call.receiveStream().bufferedReader().readText(),
                        )
                    }
                }
            }
            post<RegisterMailHandler.Request>("/api/register_mail/v1") { request ->
                val apiKey = context.request.headers["Authorization"]
                withTimeout(5.seconds) {
                    val result = RegisterMailHandler(
                        diContainer = MainDiContainer(),
                    ).handle(
                        request = request,
                        apiKey = apiKey,
                    )
                    when (result) {
                        RegisterMailHandler.Result.Forbidden -> {
                            call.respondText(
                                status = HttpStatusCode.Forbidden,
                                text = HttpStatusCode.Forbidden.value.toString(),
                            )
                        }

                        is RegisterMailHandler.Result.Success -> {
                            call.respondText(
                                contentType = ContentType.Application.Json,
                                text = Json.Default.encodeToString(
                                    result.response,
                                ),
                            )
                        }
                    }
                }
            }
        }

        File(ServerEnv.frontPath).allFiles()
            .filterNot { "index.html" == it.name }
            .forEach {
                val accessPath =
                    it.path.replace("\\", "/")
                        .removePrefix(ServerEnv.frontPath.replace("\\", "/"))
                staticFiles(
                    remotePath = accessPath,
                    dir = File(it.path),
                ) {
                    when {
                        accessPath.endsWith(".ttf") -> {
                            cacheControl {
                                listOf(
                                    CacheControl.MaxAge(maxAgeSeconds = 30.days.inWholeSeconds.toInt()),
                                )
                            }
                        }

                        accessPath == "/favicon.ico" -> {
                            cacheControl {
                                listOf(
                                    CacheControl.MaxAge(maxAgeSeconds = 1.hours.inWholeSeconds.toInt()),
                                )
                            }
                        }

                        accessPath == "/skiko.wasm" -> {
                            cacheControl {
                                listOf(
                                    CacheControl.MaxAge(maxAgeSeconds = 1.hours.inWholeSeconds.toInt()),
                                )
                            }
                        }

                        else -> Unit
                    }
                }
            }
        accept(ContentType.Text.Html) {
            get("{...}") {
                call.respondFile(
                    file = File(ServerEnv.htmlPath),
                )
            }
        }
    }
}

private fun File.allFiles(): List<File> {
    return if (isDirectory) {
        listFiles().orEmpty().map {
            it.allFiles()
        }.flatten()
    } else {
        listOf(this)
    }
}
