package net.matsudamper.money.backend.graphql

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import graphql.analysis.QueryTraverser
import graphql.analysis.QueryVisitorFieldEnvironment
import graphql.analysis.QueryVisitorStub
import graphql.execution.CoercedVariables
import graphql.parser.Parser
import graphql.schema.GraphQLSchema

/**
 * 操作が選択しているフィールドの @longRunning から、その操作に許すタイムアウトを決める
 */
class GraphqlOperationTimeout(
    private val schema: GraphQLSchema,
) {
    fun resolve(
        query: String?,
        operationName: String?,
        variables: Map<String, Any>,
    ): Duration {
        query ?: return DEFAULT_TIMEOUT
        // 不正なクエリは実行時にエラーとして返すため、ここでは通常のタイムアウトに任せる
        return runCatching {
            var maxTimeoutSeconds: Int? = null
            QueryTraverser.newQueryTraverser()
                .schema(schema)
                .document(Parser.parse(query))
                .operationName(operationName?.ifEmpty { null })
                .coercedVariables(CoercedVariables.of(variables))
                .build()
                .visitPreOrder(
                    object : QueryVisitorStub() {
                        override fun visitField(env: QueryVisitorFieldEnvironment) {
                            val timeoutSeconds = env.fieldDefinition
                                .getAppliedDirective(LONG_RUNNING_DIRECTIVE_NAME)
                                ?.getArgument(TIMEOUT_SECONDS_ARGUMENT_NAME)
                                ?.getValue<Any?>()
                                .let { it as? Number }
                                ?.toInt()
                                ?: return
                            maxTimeoutSeconds = maxOf(maxTimeoutSeconds ?: 0, timeoutSeconds)
                        }
                    },
                )
            maxTimeoutSeconds?.seconds?.coerceAtLeast(DEFAULT_TIMEOUT) ?: DEFAULT_TIMEOUT
        }.getOrDefault(DEFAULT_TIMEOUT)
    }

    companion object {
        val DEFAULT_TIMEOUT: Duration = 5.seconds
        private const val LONG_RUNNING_DIRECTIVE_NAME = "longRunning"
        private const val TIMEOUT_SECONDS_ARGUMENT_NAME = "timeoutSeconds"
    }
}
