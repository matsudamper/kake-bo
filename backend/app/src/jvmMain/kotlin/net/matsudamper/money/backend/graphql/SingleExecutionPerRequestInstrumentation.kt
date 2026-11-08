package net.matsudamper.money.backend.graphql

import graphql.ExecutionResult
import graphql.execution.AbortExecutionException
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters

/**
 * @singleExecutionPerRequest が付いたフィールドを1リクエストで複数実行させない。
 * エイリアスで複数並べると並列に実行されるため、resolver の実行前に弾く。
 * QueryTraverser は同じ fragment を複数箇所で展開しても1回しか走査しないため、正規化された操作で数える。
 */
internal class SingleExecutionPerRequestInstrumentation : Instrumentation {
    override fun beginExecuteOperation(
        parameters: InstrumentationExecuteOperationParameters,
        state: InstrumentationState?,
    ): InstrumentationContext<ExecutionResult>? {
        val executionContext = parameters.executionContext
        val schema = executionContext.graphQLSchema
        val restrictedFieldCount = executionContext.normalizedQueryTree.get()
            .coordinatesToNormalizedFields
            .entries()
            .count { (coordinates, _) ->
                schema.getFieldDefinition(coordinates)?.getAppliedDirective(DIRECTIVE_NAME) != null
            }
        if (restrictedFieldCount > 1) {
            throw AbortExecutionException("@$DIRECTIVE_NAME field can only be executed once per request")
        }
        return super.beginExecuteOperation(parameters, state)
    }

    companion object {
        private const val DIRECTIVE_NAME = "singleExecutionPerRequest"
    }
}
