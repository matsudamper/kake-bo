@file:OptIn(ApolloExperimental::class)

package net.matsudamper.money.frontend.graphql.compiler

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerRegistry
import com.apollographql.apollo.compiler.Transform
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOutput
import com.apollographql.apollo.compiler.ir.IrOperations
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * サーバーのスキーマで @longRunning が付いたフィールドを含む操作に、LongRunningOperation を実装させる
 */
class LongRunningOperationCompilerPlugin : ApolloCompilerPlugin {
    override fun beforeCompilationStep(
        environment: ApolloCompilerPluginEnvironment,
        registry: ApolloCompilerRegistry,
    ) {
        val serverSchema = environment.arguments[ARGUMENT_SERVER_SCHEMA] as? String ?: return
        val fieldTimeouts = LongRunningFieldTimeouts.parse(serverSchema)
        // IRの変換とKotlinの出力の変換は同じコード生成の中で順に呼ばれるため、IRで求めた結果を出力側で使う
        val timeoutSecondsByOperationName = mutableMapOf<String, Int>()

        registry.registerIrTransform(
            id = "$TRANSFORM_ID_PREFIX.ir",
            transform = object : Transform<IrOperations> {
                override fun transform(input: IrOperations): IrOperations {
                    input.operations.forEach { operation ->
                        val timeoutSeconds = fieldTimeouts.timeoutSecondsOf(operation.sourceWithFragments) ?: return@forEach
                        timeoutSecondsByOperationName[operation.name] = timeoutSeconds
                    }
                    return input
                }
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
        const val ARGUMENT_SERVER_SCHEMA = "net.matsudamper.money.longRunning.serverSchema"
        private const val TRANSFORM_ID_PREFIX = "net.matsudamper.money.longRunning"
        private const val TIMEOUT_SECONDS_PROPERTY_NAME = "timeoutSeconds"
        private val LONG_RUNNING_OPERATION = ClassName("net.matsudamper.money.frontend.graphql", "LongRunningOperation")
    }
}
