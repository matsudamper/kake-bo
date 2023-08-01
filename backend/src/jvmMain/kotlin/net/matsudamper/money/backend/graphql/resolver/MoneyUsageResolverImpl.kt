package net.matsudamper.money.backend.graphql.resolver

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.dataloader.MoneyUsageDataLoaderDefine
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.graphql.model.MoneyUsageResolver
import net.matsudamper.money.graphql.model.QlMoneyUsage
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategory
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

class MoneyUsageResolverImpl : MoneyUsageResolver {
    override fun title(
        moneyUsage: QlMoneyUsage,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String>> {
        val context = getContext(env)
        val userId = context.verifyUserSession()
        val futureResult = getMoneyUsageFutureResult(
            context = context,
            env = env,
            userId = userId,
            moneyUsageId = moneyUsage.id,
        )
        return CompletableFuture.supplyAsync {
            futureResult.get().title
        }.toDataFetcher()
    }

    override fun description(
        moneyUsage: QlMoneyUsage,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String>> {
        val context = getContext(env)
        val userId = context.verifyUserSession()
        val futureResult = getMoneyUsageFutureResult(
            context = context,
            env = env,
            userId = userId,
            moneyUsageId = moneyUsage.id,
        )
        return CompletableFuture.supplyAsync {
            futureResult.get().description
        }.toDataFetcher()
    }

    override fun date(
        moneyUsage: QlMoneyUsage,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<LocalDateTime>> {
        val context = getContext(env)
        val userId = context.verifyUserSession()
        val futureResult = getMoneyUsageFutureResult(
            context = context,
            env = env,
            userId = userId,
            moneyUsageId = moneyUsage.id,
        )
        return CompletableFuture.supplyAsync {
            futureResult.get().date
        }.toDataFetcher()
    }

    override fun amount(
        moneyUsage: QlMoneyUsage,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Int>> {
        val context = getContext(env)
        val userId = context.verifyUserSession()
        val futureResult = getMoneyUsageFutureResult(
            context = context,
            env = env,
            userId = userId,
            moneyUsageId = moneyUsage.id,
        )
        return CompletableFuture.supplyAsync {
            futureResult.get().amount
        }.toDataFetcher()
    }

    override fun moneyUsageSubCategory(
        moneyUsage: QlMoneyUsage,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsageSubCategory?>> {
        val context = getContext(env)
        val userId = context.verifyUserSession()
        val futureResult = getMoneyUsageFutureResult(
            context = context,
            env = env,
            userId = userId,
            moneyUsageId = moneyUsage.id,
        )
        return CompletableFuture.supplyAsync {
            val subCategoryId = futureResult.get().subCategoryId ?: return@supplyAsync null
            QlMoneyUsageSubCategory(
                id = subCategoryId,
            )
        }.toDataFetcher()
    }

    private fun getContext(env: DataFetchingEnvironment): GraphQlContext {
        return env.graphQlContext.get(GraphQlContext::class.java.name)
    }

    private fun getMoneyUsageFutureResult(
        context: GraphQlContext,
        env: DataFetchingEnvironment,
        userId: UserId,
        moneyUsageId: MoneyUsageId,
    ): CompletableFuture<MoneyUsageDataLoaderDefine.MoneyUsage> {
        return context.dataLoaders.moneyUsageDataLoader.get(env).load(
            MoneyUsageDataLoaderDefine.Key(
                userId = userId,
                moneyUsageId = moneyUsageId,
            ),
        )
    }
}
