package net.matsudamper.money.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression

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
        val calledName = expression.calleeExpression?.text ?: return
        val calledFqName = expression.containingKtFile.resolveDetectionTargetFqName(calledName) ?: return

        val isReflectiveBodyCall = calledFqName in bodyConversionFqNames && !expression.isStatusOnlyResponse()
        val isTypedRouteBuilder = calledFqName in routeBuilderFqNames && expression.hasRequestBodyType()

        if (isReflectiveBodyCall || isTypedRouteBuilder) {
            report(
                Finding(
                    entity = Entity.from(expression),
                    message = "$calledFqName は実行時に serializer を探すため native image で失敗する。" +
                        "serializer() を明示して respondText / receiveText と組み合わせる",
                ),
            )
        }
    }

    /**
     * HttpStatusCode だけを渡す respond は本文を持たず、serializer 探索をしない。
     * OutgoingContent などは PSI だけでは判別できないため、必要なら Suppress する。
     */
    private fun KtCallExpression.isStatusOnlyResponse(): Boolean {
        val arguments = valueArguments.mapNotNull { it.getArgumentExpression() }
        return arguments.isNotEmpty() && arguments.all { it.text.startsWith("HttpStatusCode.") }
    }

    /**
     * リクエストボディ付きのオーバーロードは、型引数を省略してもラムダ引数の型から推論される。
     * 関数参照などは PSI だけでは型が分からないため、引数なしのラムダ以外は検知対象にする。
     */
    private fun KtCallExpression.hasRequestBodyType(): Boolean {
        if (typeArguments.isNotEmpty()) return true
        val handlerArgument = valueArguments.firstOrNull { it.getArgumentName()?.asName?.asString() == "body" }
            ?: valueArguments.lastOrNull { it.getArgumentName() == null }
        val handler = handlerArgument?.getArgumentExpression() ?: return false
        val handlerLambda = handler as? KtLambdaExpression ?: return true
        return handlerLambda.valueParameters.isNotEmpty()
    }

    /**
     * 同名の callable が複数 import されていると、どれが呼ばれるかは PSI だけでは決まらない。
     * そのため検知対象の FQ 名が候補に含まれていれば、それを返す。
     */
    private fun KtFile.resolveDetectionTargetFqName(calledName: String): String? {
        val explicitCandidates = importDirectives
            .filterNot { it.isAllUnder }
            .mapNotNull { directive ->
                val fqName = directive.importedFqName ?: return@mapNotNull null
                val localName = directive.aliasName ?: fqName.shortName().asString()
                fqName.asString().takeIf { localName == calledName }
            }
        val candidates = explicitCandidates.ifEmpty {
            importDirectives
                .filter { it.isAllUnder }
                .mapNotNull { it.importedFqName?.asString() }
                .map { packageName -> "$packageName.$calledName" }
        }
        return candidates.firstOrNull { it in bodyConversionFqNames || it in routeBuilderFqNames }
    }

    private companion object {
        private val bodyConversionFqNames = setOf(
            "io.ktor.server.response.respond",
            "io.ktor.server.response.respondNullable",
            "io.ktor.server.request.receive",
            "io.ktor.server.request.receiveNullable",
        )

        private val routeBuilderFqNames = setOf("get", "post", "put", "patch", "delete", "head", "options")
            .map { "io.ktor.server.routing.$it" }
            .toSet()
    }
}
