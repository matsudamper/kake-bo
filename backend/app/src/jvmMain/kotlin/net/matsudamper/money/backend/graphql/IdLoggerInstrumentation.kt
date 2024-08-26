package net.matsudamper.money.backend.graphql

import kotlin.reflect.full.memberProperties
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters
import graphql.schema.DataFetcher
import net.matsudamper.money.backend.base.TraceLogger

internal class IdLoggerInstrumentation(
    private val tracer: TraceLogger,
) : Instrumentation {
    override fun instrumentDataFetcher(
        dataFetcher: DataFetcher<*>?,
        parameters: InstrumentationFieldFetchParameters?,
        state: InstrumentationState?,
    ): DataFetcher<*> {
        return DataFetcher { environment ->
            val source: Any? = environment.getSource<Any>()
            if (source != null) {
                val id = source::class.memberProperties
                    .firstOrNull { it.name == "id" }
                    ?.getter?.call(source)

                if (id != null) {
                    tracer.setTag("id", id.toString())
                }
            }

            return@DataFetcher dataFetcher?.get(environment)
        }
    }
}
