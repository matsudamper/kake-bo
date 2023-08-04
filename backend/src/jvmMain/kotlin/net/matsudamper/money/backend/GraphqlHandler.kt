package net.matsudamper.money.backend

import java.lang.reflect.UndeclaredThrowableException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import graphql.ExceptionWhileDataFetching
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQLError
import graphql.InvalidSyntaxError
import graphql.execution.NonNullableFieldWasNullError
import graphql.validation.ValidationError
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveStream
import net.matsudamper.money.backend.base.ObjectMapper
import net.matsudamper.money.backend.di.RepositoryFactoryImpl
import net.matsudamper.money.backend.exception.GraphQlMultiException
import net.matsudamper.money.backend.graphql.DataLoaders
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.GraphqlMoneyException
import net.matsudamper.money.backend.graphql.MoneyGraphQlSchema
import org.dataloader.DataLoaderRegistry

class GraphqlHandler(
    private val call: ApplicationCall,
) {
    suspend fun handle(): String {
        val requestText = call.receiveStream().bufferedReader().readText()
        val request = jacksonObjectMapper().readValue<GraphQlRequest>(requestText)

        val repositoryFactory = RepositoryFactoryImpl()
        val dataLoaderRegistryBuilder = DataLoaderRegistry.Builder()
        val dataLoaders = DataLoaders(
            repositoryFactory = repositoryFactory,
            dataLoaderRegistryBuilder = dataLoaderRegistryBuilder,
        )
        val executionInputBuilder = ExecutionInput.newExecutionInput()
            .dataLoaderRegistry(
                dataLoaderRegistryBuilder.build(),
            )
            .graphQLContext(
                mapOf(
                    GraphQlContext::class.java.name to GraphQlContext(
                        call = call,
                        repositoryFactory = repositoryFactory,
                        dataLoaders = dataLoaders,
                    ),
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

        return ObjectMapper.jackson.writeValueAsString(responseResult)
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

                is Throwable -> it

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
}
