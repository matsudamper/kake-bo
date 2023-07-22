package net.matsudamper.money.backend

import java.io.File
import java.lang.reflect.UndeclaredThrowableException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import graphql.ExceptionWhileDataFetching
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQLError
import graphql.InvalidSyntaxError
import graphql.execution.NonNullableFieldWasNullError
import graphql.execution.NonNullableValueCoercedAsNullException
import graphql.validation.ValidationError
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.file
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
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
import net.matsudamper.money.backend.base.CustomLogger
import net.matsudamper.money.backend.base.ObjectMapper
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.exception.GraphQlMultiException
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.GraphqlMoneyException
import net.matsudamper.money.backend.graphql.MoneyGraphQlSchema
import org.slf4j.event.Level

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty("logback.configurationFile", "logback.xml")

            // Initialize
            MoneyGraphQlSchema.graphql

            embeddedServer(
                CIO,
                port = ServerEnv.port,
                module = Application::myApplicationModule,
                configure = {

                },
            ).start(wait = true)
        }
    }
}

fun Application.myApplicationModule() {
    install(ForwardedHeaders)
    install(XForwardedHeaders)
    install(ContentNegotiation) {
        json(
            json = ObjectMapper.kotlinxSerialization,
            contentType = ContentType.Application.Json,
        )
    }
    install(CallLogging) {
        level = Level.WARN
        filter { call ->
            CustomLogger.General.debug(
                buildString {
                    appendLine("==========${call.request.path()}==========")
                    appendLine(
                        call.request.headers.entries().joinToString("\n") { (key, value) ->
                            "$key=$value"
                        },
                    )
                },
            )
            true
        }
    }

    routing {
        accept(ContentType.Application.Json) {
            post("/query") {
                val requestText = call.receiveStream().bufferedReader().readText()
                val request = jacksonObjectMapper().readValue<GraphQlRequest>(requestText)

                val executionInputBuilder = ExecutionInput.newExecutionInput()
                    .graphQLContext(
                        mapOf(
                            GraphQlContext::class.java.name to GraphQlContext(call),
                        ),
                    )
                    .query(request.query)
                    .variables(request.variables)

                val result = MoneyGraphQlSchema.graphql
                    .execute(executionInputBuilder)

                val handleError = handleError(result.errors)

                val responseResult = ExecutionResult.newExecutionResult()
                    .data(result.getData())
                    .extensions(
                        mapOf(
                            "errors" to handleError.map { e ->
                                when (e) {
                                    is GraphqlMoneyException.SessionNotVerify -> {
                                        "SessionNotVerify"
                                    }
                                }
                            },
                        ),
                    )
                    .build()

                call.respondText(
                    contentType = ContentType.Application.Json,
                    text = ObjectMapper.jackson.writeValueAsString(responseResult),
                )
            }
        }

        File(ServerEnv.frontPath).allFiles()
            .filterNot { "index.html" == it.name }
            .forEach {
                val accessPath = it.path.replace("\\", "/").removePrefix(ServerEnv.frontPath)
                file(accessPath, it.path)
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

private fun handleError(errors: MutableList<GraphQLError>): List<GraphqlMoneyException> {
    val graphqlMoneyExceptions = mutableListOf<GraphqlMoneyException>()
    val exceptions = errors.mapNotNull {
        when (it) {
            is ExceptionWhileDataFetching -> {
                runCatching {
                    when (val e = it.exception) {
                        is UndeclaredThrowableException -> {
                            when (val undeclaredThrowable = e.undeclaredThrowable) {
                                is GraphqlMoneyException -> {
                                    graphqlMoneyExceptions.add(undeclaredThrowable)
                                    return@runCatching null
                                }

                                else -> Unit
                            }
                        }

                        else -> Unit
                    }

                    throw IllegalStateException(
                        it.message,
                        it.exception,
                    )
                }.fold(
                    onSuccess = { null },
                    onFailure = { it },
                )
            }

            is ValidationError -> {
                IllegalStateException(it.message)
            }

            is InvalidSyntaxError -> {
                IllegalStateException(it.message)
            }

            is NonNullableFieldWasNullError -> {
                IllegalStateException(
                    "NonNullableFieldWasNullError: message=${it.message}, path=${it.path}",
                )
            }

            is NonNullableValueCoercedAsNullException -> {
                IllegalStateException(
                    "NonNullableValueCoercedAsNullException: message=${it.message}, path=${it.path}",
                )
            }

            else -> {
                IllegalStateException(
                    "NotHandleError:$it message=${it.message}, path=${it.path}",
                )
            }
        }
    }

    if (exceptions.isNotEmpty()) {
        throw GraphQlMultiException(exceptions)
    }

    return graphqlMoneyExceptions
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
