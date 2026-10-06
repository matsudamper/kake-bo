package net.matsudamper.money.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Ktor の ContentNegotiation は KType から実行時に serializer を探すため、
 * native image ではリフレクション情報不足で SerializationException や KotlinReflectionInternalError になる。
 */
class KtorRuntimeSerializerLookup(config: Config) : Rule(
    config,
    "Ktor の型推論シリアライズは native image で失敗するため、serializer() を明示して encode/decode する",
) {
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val functionName = expression.calleeExpression?.text ?: return
        val importedFqNames = expression.containingKtFile.importDirectives
            .mapNotNull { directive ->
                val fqName = directive.importedFqName?.asString() ?: return@mapNotNull null
                if (directive.isAllUnder) "$fqName.*" else fqName
            }
            .toSet()

        val isReflectiveBodyCall = bodyConversionFunctions.any { (packageName, names) ->
            functionName in names && importedFqNames.isImported(packageName, functionName)
        }
        val isTypedRouteBuilder = functionName in typedRouteBuilderNames &&
            expression.typeArguments.isNotEmpty() &&
            importedFqNames.isImported(ROUTING_PACKAGE, functionName)

        if (isReflectiveBodyCall || isTypedRouteBuilder) {
            report(
                Finding(
                    entity = Entity.from(expression),
                    message = "$functionName は実行時に serializer を探すため native image で失敗する。" +
                        "serializer() を明示して respondText / receiveText と組み合わせる",
                ),
            )
        }
    }

    private fun Set<String>.isImported(packageName: String, functionName: String): Boolean {
        return "$packageName.$functionName" in this || "$packageName.*" in this
    }

    private companion object {
        private const val ROUTING_PACKAGE = "io.ktor.server.routing"

        private val bodyConversionFunctions = mapOf(
            "io.ktor.server.response" to setOf("respond"),
            "io.ktor.server.request" to setOf("receive", "receiveNullable"),
        )

        private val typedRouteBuilderNames = setOf("get", "post", "put", "patch", "delete", "head", "options")
    }
}
