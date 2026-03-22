package net.matsudamper.money.backend.graphql

import java.util.concurrent.CompletionStage
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters
import graphql.schema.DataFetcher
import graphql.schema.GraphQLTypeUtil
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context

internal class OtelInstrumentation(
    openTelemetry: OpenTelemetry,
) : Instrumentation {
    private val tracer = openTelemetry.getTracer("graphql-field")

    override fun instrumentDataFetcher(
        dataFetcher: DataFetcher<*>?,
        parameters: InstrumentationFieldFetchParameters?,
        state: InstrumentationState?,
    ): DataFetcher<*> {
        if (parameters?.isTrivialDataFetcher == true) {
            return DataFetcher { environment ->
                val context = Context.current()
                context.makeCurrent().use {
                    dataFetcher?.get(environment)
                }
            }
        }

        val parentTypeName = parameters?.executionStepInfo?.parent?.type?.let {
            GraphQLTypeUtil.simplePrint(it)
        }.orEmpty()
        val fieldName = parameters?.executionStepInfo?.field?.name.orEmpty()

        return DataFetcher { environment ->
            val span = tracer.spanBuilder("$parentTypeName.$fieldName").startSpan()
            try {
                span.makeCurrent().use {
                    val result = dataFetcher?.get(environment)
                    if (result is CompletionStage<*>) {
                        result.whenComplete { _, error ->
                            if (error != null) {
                                span.setStatus(StatusCode.ERROR)
                                span.recordException(error)
                            }
                            span.end()
                        }
                        result
                    } else {
                        span.end()
                        result
                    }
                }
            } catch (e: Exception) {
                span.setStatus(StatusCode.ERROR)
                span.recordException(e)
                span.end()
                throw e
            }
        }
    }
}
