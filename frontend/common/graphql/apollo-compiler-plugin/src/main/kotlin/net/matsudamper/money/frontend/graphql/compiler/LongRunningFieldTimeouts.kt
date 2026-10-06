package net.matsudamper.money.frontend.graphql.compiler

import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFieldDefinition
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLIntValue
import com.apollographql.apollo.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.rawType

/**
 * スキーマの @longRunning を元に、操作に必要なタイムアウトを求める
 */
internal class LongRunningFieldTimeouts(
    private val schema: Schema,
) {
    /**
     * @return @longRunning のフィールドを含まない場合はnull
     */
    fun timeoutSecondsOf(
        operation: GQLOperationDefinition,
        fragments: Map<String, GQLFragmentDefinition>,
    ): Int? {
        val rootTypeName = schema.rootTypeNameOrNullFor(operation.operationType) ?: return null
        return maxTimeoutSeconds(
            selections = operation.selections,
            parentTypeName = rootTypeName,
            fragments = fragments,
            visitedFragmentNames = mutableSetOf(),
        )
    }

    private fun maxTimeoutSeconds(
        selections: List<GQLSelection>,
        parentTypeName: String,
        fragments: Map<String, GQLFragmentDefinition>,
        visitedFragmentNames: MutableSet<String>,
    ): Int? {
        return selections.mapNotNull { selection ->
            when (selection) {
                is GQLField -> {
                    val field = fieldDefinition(typeName = parentTypeName, fieldName = selection.name) ?: return@mapNotNull null
                    val childTimeoutSeconds = maxTimeoutSeconds(
                        selections = selection.selections,
                        parentTypeName = field.type.rawType().name,
                        fragments = fragments,
                        visitedFragmentNames = visitedFragmentNames,
                    )
                    listOfNotNull(field.longRunningTimeoutSeconds(), childTimeoutSeconds).maxOrNull()
                }

                is GQLInlineFragment -> maxTimeoutSeconds(
                    selections = selection.selections,
                    parentTypeName = selection.typeCondition?.name ?: parentTypeName,
                    fragments = fragments,
                    visitedFragmentNames = visitedFragmentNames,
                )

                is GQLFragmentSpread -> {
                    if (visitedFragmentNames.add(selection.name).not()) return@mapNotNull null
                    val fragment = fragments[selection.name] ?: return@mapNotNull null
                    maxTimeoutSeconds(
                        selections = fragment.selections,
                        parentTypeName = fragment.typeCondition.name,
                        fragments = fragments,
                        visitedFragmentNames = visitedFragmentNames,
                    )
                }
            }
        }.maxOrNull()
    }

    private fun fieldDefinition(
        typeName: String,
        fieldName: String,
    ): GQLFieldDefinition? {
        val fields = when (val typeDefinition = schema.typeDefinitions[typeName]) {
            is GQLObjectTypeDefinition -> typeDefinition.fields
            is GQLInterfaceTypeDefinition -> typeDefinition.fields
            else -> return null
        }
        return fields.firstOrNull { it.name == fieldName }
    }

    private fun GQLFieldDefinition.longRunningTimeoutSeconds(): Int? {
        val value = directives.firstOrNull { it.name == DIRECTIVE_NAME }
            ?.arguments
            ?.firstOrNull { it.name == TIMEOUT_SECONDS_ARGUMENT_NAME }
            ?.value
            ?: return null
        val timeoutSeconds = checkNotNull((value as? GQLIntValue)?.value?.toIntOrNull()) {
            "@$DIRECTIVE_NAME の $TIMEOUT_SECONDS_ARGUMENT_NAME には整数を指定してください: $name"
        }
        check(timeoutSeconds in MIN_TIMEOUT_SECONDS..MAX_TIMEOUT_SECONDS) {
            "@$DIRECTIVE_NAME の $TIMEOUT_SECONDS_ARGUMENT_NAME は通常のタイムアウト以上かつ通信エンジンのタイムアウト以下にするため、" +
                "$MIN_TIMEOUT_SECONDS..${MAX_TIMEOUT_SECONDS}の範囲にしてください: $name"
        }
        return timeoutSeconds
    }

    private companion object {
        private const val DIRECTIVE_NAME = "longRunning"
        private const val TIMEOUT_SECONDS_ARGUMENT_NAME = "timeoutSeconds"

        /**
         * 通常の操作のタイムアウト（OperationTimeoutInterceptor の DEFAULT_OPERATION_TIMEOUT）に合わせる
         */
        private const val MIN_TIMEOUT_SECONDS = 5

        /**
         * クライアントの通信エンジンのタイムアウト（GraphqlClient の HTTP_ENGINE_TIMEOUT）に合わせる
         */
        private const val MAX_TIMEOUT_SECONDS = 120
    }
}
