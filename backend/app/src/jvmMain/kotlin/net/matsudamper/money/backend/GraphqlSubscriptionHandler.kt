package net.matsudamper.money.backend

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import graphql.ExecutionInput
import graphql.ExecutionResult
import io.opentelemetry.context.Context
import net.matsudamper.money.backend.base.CookieManager
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.backend.feature.session.UserSessionManagerImpl
import net.matsudamper.money.backend.graphql.DataLoaders
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.MoneyGraphQlSchema
import org.dataloader.DataLoaderRegistry
import org.reactivestreams.Publisher

/**
 * Subscription を実行してイベントの Flow にする。Flow のキャンセルがそのまま処理の停止になる。
 */
class GraphqlSubscriptionHandler(
    private val cookieManager: CookieManager,
    private val diContainer: DiContainer,
) {
    sealed interface Result {
        /** 各要素は GraphQL レスポンスの仕様形式（data / errors） */
        data class Stream(val events: Flow<Map<String, Any?>>) : Result

        /** 認証エラーなどで購読を開始できなかった */
        data class Error(val errors: List<Map<String, Any?>>) : Result
    }

    fun subscribe(
        query: String,
        operationName: String?,
        variables: Map<String, Any?>,
    ): Result {
        val dataLoaderRegistryBuilder = DataLoaderRegistry.Builder()
        val userSessionManager = UserSessionManagerImpl(
            cookieManager = cookieManager,
            userSessionRepository = diContainer.createUserSessionRepository(),
        )
        val graphqlContext = GraphQlContext(
            cookieManager = cookieManager,
            dataLoaders = DataLoaders(
                diContainer = diContainer,
                dataLoaderRegistryBuilder = dataLoaderRegistryBuilder,
                userSessionManager = userSessionManager,
                otelContext = Context.current(),
            ),
            userSessionManager = userSessionManager,
            diContainer = diContainer,
        )
        val executionInput = ExecutionInput.newExecutionInput()
            .dataLoaderRegistry(dataLoaderRegistryBuilder.build())
            .graphQLContext(mapOf(GraphQlContext::class.java.name to graphqlContext))
            .query(query)
            .operationName(operationName)
            .variables(variables)

        val result = MoneyGraphQlSchema.graphql.execute(executionInput)
        val publisher = result.getData<Publisher<ExecutionResult>?>()
            ?: return Result.Error(result.errors.map { it.toSpecification() })

        return Result.Stream(
            publisher.asFlow().map { it.toSpecification() },
        )
    }
}
