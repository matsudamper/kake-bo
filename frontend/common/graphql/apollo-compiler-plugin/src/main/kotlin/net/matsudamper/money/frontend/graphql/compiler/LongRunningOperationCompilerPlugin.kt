@file:OptIn(ApolloExperimental::class)

package net.matsudamper.money.frontend.graphql.compiler

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerRegistry
import com.apollographql.apollo.compiler.ExecutableDocumentTransform
import com.apollographql.apollo.compiler.Transform
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOutput
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * スキーマで @longRunning が付いたフィールドを含む操作に、LongRunningOperation を実装させる
 */
class LongRunningOperationCompilerPlugin : ApolloCompilerPlugin {
    override fun beforeCompilationStep(
        environment: ApolloCompilerPluginEnvironment,
        registry: ApolloCompilerRegistry,
    ) {
        // 操作の変換とKotlinの出力の変換は同じコード生成の中で順に呼ばれるため、操作の変換で求めた結果を出力側で使う
        val timeoutSecondsByOperationName = mutableMapOf<String, Int>()

        registry.registerExecutableDocumentTransform(
            id = "$TRANSFORM_ID_PREFIX.document",
            transform = ExecutableDocumentTransform { schema, document, extraFragmentDefinitions ->
                val fieldTimeouts = LongRunningFieldTimeouts(schema)
                val fragments = (document.definitions.filterIsInstance<GQLFragmentDefinition>() + extraFragmentDefinitions)
                    .associateBy { it.name }
                document.definitions.filterIsInstance<GQLOperationDefinition>().forEach { operation ->
                    val operationName = operation.name ?: return@forEach
                    val timeoutSeconds = fieldTimeouts.timeoutSecondsOf(operation = operation, fragments = fragments) ?: return@forEach
                    timeoutSecondsByOperationName[operationName] = timeoutSeconds
                }
                document
            },
        )
        registry.registerKotlinOutputTransform(
            id = "$TRANSFORM_ID_PREFIX.kotlin",
            transform = object : Transform<KotlinOutput> {
                override fun transform(input: KotlinOutput): KotlinOutput {
                    return KotlinOutput(
                        fileSpecs = input.fileSpecs.map { it.withLongRunningOperation(timeoutSecondsByOperationName) },
                        codegenMetadata = input.codegenMetadata,
                    )
                }
            },
        )
    }

    private fun FileSpec.withLongRunningOperation(timeoutSecondsByOperationName: Map<String, Int>): FileSpec {
        val builder = toBuilder()
        var isChanged = false
        builder.members.replaceAll { member ->
            if (member !is TypeSpec) return@replaceAll member
            val timeoutSeconds = member.operationName()?.let(timeoutSecondsByOperationName::get) ?: return@replaceAll member
            isChanged = true
            member.toBuilder()
                .addSuperinterface(LONG_RUNNING_OPERATION)
                .addProperty(
                    PropertySpec.builder(TIMEOUT_SECONDS_PROPERTY_NAME, INT, KModifier.OVERRIDE)
                        .getter(FunSpec.getterBuilder().addStatement("return %L", timeoutSeconds).build())
                        .build(),
                )
                .build()
        }
        return if (isChanged) builder.build() else this
    }

    /**
     * Apolloが生成する操作クラスは、companion object の OPERATION_NAME に操作名を持つ
     */
    private fun TypeSpec.operationName(): String? {
        return typeSpecs.firstOrNull { it.isCompanion }
            ?.propertySpecs
            ?.firstOrNull { it.name == "OPERATION_NAME" }
            ?.initializer
            ?.toString()
            ?.removeSurrounding("\"")
    }

    companion object {
        private const val TRANSFORM_ID_PREFIX = "net.matsudamper.money.longRunning"
        private const val TIMEOUT_SECONDS_PROPERTY_NAME = "timeoutSeconds"
        private val LONG_RUNNING_OPERATION = ClassName("net.matsudamper.money.frontend.graphql", "LongRunningOperation")
    }
}
