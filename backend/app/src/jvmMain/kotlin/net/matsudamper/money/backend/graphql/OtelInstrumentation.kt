package net.matsudamper.money.backend.graphql

import graphql.execution.instrumentation.Instrumentation
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters
import graphql.schema.DataFetcher
import io.opentelemetry.context.Context

internal class OtelInstrumentation : Instrumentation {
    override fun instrumentDataFetcher(
        dataFetcher: DataFetcher<*>?,
        parameters: InstrumentationFieldFetchParameters?,
        state: InstrumentationState?,
    ): DataFetcher<*> {
        return DataFetcher { environment ->
            val context = Context.current()
            return@DataFetcher context.makeCurrent().use {
                dataFetcher?.get(environment)
            }
        }
    }
}
