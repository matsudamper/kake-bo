package net.matsudamper.money.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * reified な serializer 探索は native image で SerializationException や KotlinReflectionInternalError になる。
 * 関数名ではなく、解決したシグネチャが inline reified かどうかで判定する。
 */
class KtorRuntimeSerializerLookup(config: Config) :
    Rule(
        config,
        "型推論によるシリアライズは native image で失敗するため、KSerializer を明示して encode/decode する",
    ),
    RequiresAnalysisApi {
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        // Detekt 同梱の Kotlin 2.4.10 では graphql-java の JSpecify 型解決が再帰し続ける。
        if (expression.text.contains("graphQlContext.get")) return
        analyze(expression) {
            val call = expression.resolveToCall()?.successfulFunctionCallOrNull() ?: return
            val symbol = call.signature.symbol as? KaNamedFunctionSymbol ?: return
            if (!isRuntimeSerializerLookup(symbol, call)) return
            val callableId = symbol.callableId?.asSingleFqName()?.asString() ?: symbol.name.asString()
            report(
                Finding(
                    entity = Entity.from(expression),
                    message = "$callableId は実行時に serializer を探すため native image で失敗する。" +
                        "KSerializer を明示するか、respondText / receiveText を使う",
                ),
            )
        }
    }

    /**
     * HttpStatusCode だけを渡す respond は DefaultTransform がステータス設定に使い、本文の serializer 探索をしない。
     */
    private fun isRuntimeSerializerLookup(symbol: KaNamedFunctionSymbol, call: KaFunctionCall<*>): Boolean {
        if (!symbol.isInline || symbol.typeParameters.none { it.isReified }) return false
        if (symbol.valueParameters.any { it.returnType.referencesClass(serializerClassIds) }) return false

        val packageName = symbol.callableId?.packageName?.asString() ?: return false
        if (packageName.isWithinPackage("kotlinx.serialization")) {
            return call.hasSerialFormatReceiver() || symbol.returnType.referencesClass(kSerializerClassIds)
        }
        if (ktorRuntimeTypeInfoPackagePrefixes.any { packageName.isWithinPackage(it) }) {
            return !isStatusCodeOnly(symbol, call)
        }
        return false
    }

    private fun KaFunctionCall<*>.hasSerialFormatReceiver(): Boolean {
        val receiverTypes = listOfNotNull(
            dispatchReceiver?.type,
            extensionReceiver?.type,
        )
        return receiverTypes.any { it.referencesClass(serialFormatClassIds) }
    }

    private fun isStatusCodeOnly(symbol: KaNamedFunctionSymbol, call: KaFunctionCall<*>): Boolean {
        val reifiedParameters = symbol.typeParameters.filter { it.isReified }
        if (reifiedParameters.isEmpty()) return false
        return reifiedParameters.all { parameter ->
            val typeArgument = call.typeArgumentsMapping[parameter] ?: return@all false
            typeArgument.referencesClass(httpStatusCodeClassIds)
        }
    }

    private fun KaType.referencesClass(targets: Set<ClassId>): Boolean {
        return referencesClass(targets, emptySet())
    }

    private fun KaType.referencesClass(targets: Set<ClassId>, seen: Set<ClassId>): Boolean {
        val classType = this as? KaClassType ?: return false
        val classId = classType.classId
        if (classId in seen) return false
        if (classId in targets) return true
        val classSymbol = classType.symbol as? KaClassSymbol ?: return false
        val nextSeen = seen + classId
        return classSymbol.superTypes.any { superType -> superType.referencesClass(targets, nextSeen) }
    }

    private fun String.isWithinPackage(packageName: String): Boolean {
        return this == packageName || startsWith("$packageName.")
    }

    private companion object {
        private val serialFormatClassIds = setOf(
            ClassId.topLevel(FqName("kotlinx.serialization.SerialFormat")),
            ClassId.topLevel(FqName("kotlinx.serialization.StringFormat")),
            ClassId.topLevel(FqName("kotlinx.serialization.BinaryFormat")),
        )
        private val serializerClassIds = setOf(
            ClassId.topLevel(FqName("kotlinx.serialization.SerializationStrategy")),
            ClassId.topLevel(FqName("kotlinx.serialization.DeserializationStrategy")),
            ClassId.topLevel(FqName("kotlinx.serialization.KSerializer")),
        )
        private val kSerializerClassIds = setOf(
            ClassId.topLevel(FqName("kotlinx.serialization.KSerializer")),
        )
        private val httpStatusCodeClassIds = setOf(
            ClassId.topLevel(FqName("io.ktor.http.HttpStatusCode")),
        )
        private val ktorRuntimeTypeInfoPackagePrefixes = listOf(
            "io.ktor.server.response",
            "io.ktor.server.request",
            "io.ktor.server.routing",
            "io.ktor.server.websocket",
            "io.ktor.client.call",
            "io.ktor.client.request",
            "io.ktor.client.plugins.websocket",
            "io.ktor.client.plugins.sse",
            "io.ktor.serialization",
        )
    }
}
