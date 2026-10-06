package net.matsudamper.money.frontend.graphql.compiler

import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFieldDefinition
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLIntValue
import com.apollographql.apollo.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLObjectTypeExtension
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLSchemaDefinition
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.rawType

/**
 * サーバーのスキーマの @longRunning を元に、操作に必要なタイムアウトを求める。
 * イントロスペクションで取得したクライアントのスキーマにはフィールドのディレクティブが含まれないため、サーバーのスキーマを直接読む。
 */
internal class LongRunningFieldTimeouts private constructor(
    private val fieldsByTypeName: Map<String, Map<String, FieldInfo>>,
    private val rootTypeNameByOperationType: Map<String, String>,
) {
    /**
     * @param document フラグメントを含む操作のドキュメント
     * @return @longRunning のフィールドを含まない場合はnull
     */
    fun timeoutSecondsOf(document: String): Int? {
        val definitions = document.parseAsGQLDocument().getOrThrow().definitions
        val operation = definitions.filterIsInstance<GQLOperationDefinition>().singleOrNull() ?: return null
        val rootTypeName = rootTypeNameByOperationType[operation.operationType] ?: return null
        return maxTimeoutSeconds(
            selections = operation.selections,
            parentTypeName = rootTypeName,
            fragments = definitions.filterIsInstance<GQLFragmentDefinition>().associateBy { it.name },
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
                    val field = fieldsByTypeName[parentTypeName]?.get(selection.name) ?: return@mapNotNull null
                    val childTimeoutSeconds = maxTimeoutSeconds(
                        selections = selection.selections,
                        parentTypeName = field.typeName,
                        fragments = fragments,
                        visitedFragmentNames = visitedFragmentNames,
                    )
                    listOfNotNull(field.timeoutSeconds, childTimeoutSeconds).maxOrNull()
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

                else -> null
            }
        }.maxOrNull()
    }

    private class FieldInfo(
        val typeName: String,
        val timeoutSeconds: Int?,
    )

    companion object {
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

        fun parse(serverSchema: String): LongRunningFieldTimeouts {
            val definitions = serverSchema.parseAsGQLDocument().getOrThrow().definitions
            val fieldsByTypeName = mutableMapOf<String, MutableMap<String, FieldInfo>>()
            definitions.forEach { definition ->
                val (typeName, fields) = when (definition) {
                    is GQLObjectTypeDefinition -> definition.name to definition.fields
                    is GQLObjectTypeExtension -> definition.name to definition.fields
                    is GQLInterfaceTypeDefinition -> definition.name to definition.fields
                    else -> return@forEach
                }
                val fieldsByName = fieldsByTypeName.getOrPut(typeName) { mutableMapOf() }
                fields.forEach { field ->
                    fieldsByName[field.name] = FieldInfo(
                        typeName = field.type.rawType().name,
                        timeoutSeconds = field.longRunningTimeoutSeconds(),
                    )
                }
            }
            val rootTypeNames = definitions.filterIsInstance<GQLSchemaDefinition>()
                .flatMap { it.rootOperationTypeDefinitions }
                .associate { it.operationType to it.namedType }
            return LongRunningFieldTimeouts(
                fieldsByTypeName = fieldsByTypeName,
                rootTypeNameByOperationType = DEFAULT_ROOT_TYPE_NAMES + rootTypeNames,
            )
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

        private val DEFAULT_ROOT_TYPE_NAMES = mapOf(
            "query" to "Query",
            "mutation" to "Mutation",
            "subscription" to "Subscription",
        )
    }
}
