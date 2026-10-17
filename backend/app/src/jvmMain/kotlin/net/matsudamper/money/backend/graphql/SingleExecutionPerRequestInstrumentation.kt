package net.matsudamper.money.backend.graphql

import graphql.ExecutionResult
import graphql.analysis.QueryTraverser
import graphql.analysis.QueryVisitorFieldEnvironment
import graphql.analysis.QueryVisitorStub
import graphql.execution.AbortExecutionException
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters

/**
 * @singleExecutionPerRequest が付いたフィールドを1リクエストで複数実行させない。
 * エイリアスで複数並べると並列に実行されるため、resolver の実行前に弾く。
 */
internal class SingleExecutionPerRequestInstrumentation : Instrumentation {
    override fun beginExecuteOperation(
        parameters: InstrumentationExecuteOperationParameters,
        state: InstrumentationState?,
    ): InstrumentationContext<ExecutionResult>? {
        val executionContext = parameters.executionContext
        var restrictedFieldCount = 0
        QueryTraverser.newQueryTraverser()
            .schema(executionContext.graphQLSchema)
            .document(executionContext.document)
            .operationName(executionContext.operationDefinition.name)
            .coercedVariables(executionContext.coercedVariables)
            .build()
            .visitPreOrder(
                object : QueryVisitorStub() {
                    override fun visitField(env: QueryVisitorFieldEnvironment) {
                        if (env.fieldDefinition.getAppliedDirective(DIRECTIVE_NAME) != null) {
                            restrictedFieldCount++
                        }
                    }
                },
            )
        if (restrictedFieldCount > 1) {
            throw AbortExecutionException("@$DIRECTIVE_NAME field can only be executed once per request")
        }
        return super.beginExecuteOperation(parameters, state)
    }

    companion object {
        private const val DIRECTIVE_NAME = "singleExecutionPerRequest"
    }
}
