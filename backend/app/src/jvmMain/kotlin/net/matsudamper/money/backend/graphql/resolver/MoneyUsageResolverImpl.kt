package net.matsudamper.money.backend.graphql.resolver

import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.dataloader.MoneyUsageAssociateByImportedMailDataLoaderDefine
import net.matsudamper.money.backend.dataloader.MoneyUsageDataLoaderDefine
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.UserId
import net.matsudamper.money.graphql.model.MoneyUsageResolver
import net.matsudamper.money.graphql.model.QlImportedMail
import net.matsudamper.money.graphql.model.QlMoneyUsage
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategory

class MoneyUsageResolverImpl : MoneyUsageResolver {
    override fun title(
        moneyUsage: QlMoneyUsage,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String>> {
        val context = getContext(env)
        val userId = context.verifyUserSessionAndGetUserId()
        return getMoneyUsageFutureResult(
            context = context,
            env = env,
            userId = userId,
            moneyUsageId = moneyUsage.id,
        ).thenApplyAsync { futureResult ->
            futureResult.title
        }.toDataFetcher()
    }

    override fun description(
        moneyUsage: QlMoneyUsage,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String>> {
        val context = getContext(env)
        val userId = context.verifyUserSessionAndGetUserId()
        return getMoneyUsageFutureResult(
            context = context,
            env = env,
            userId = userId,
            moneyUsageId = moneyUsage.id,
        ).thenApplyAsync { futureResult ->
            futureResult.description
        }.toDataFetcher()
    }

    override fun date(
        moneyUsage: QlMoneyUsage,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<LocalDateTime>> {
        val context = getContext(env)
        val userId = context.verifyUserSessionAndGetUserId()
        return getMoneyUsageFutureResult(
            context = context,
            env = env,
            userId = userId,
            moneyUsageId = moneyUsage.id,
        ).thenApplyAsync { futureResult ->
            futureResult.date
        }.toDataFetcher()
    }

    override fun amount(
        moneyUsage: QlMoneyUsage,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Int>> {
        val context = getContext(env)
        val userId = context.verifyUserSessionAndGetUserId()
        return getMoneyUsageFutureResult(
            context = context,
            env = env,
            userId = userId,
            moneyUsageId = moneyUsage.id,
        ).thenApplyAsync { futureResult ->
            futureResult.amount
        }.toDataFetcher()
    }

    override fun moneyUsageSubCategory(
        moneyUsage: QlMoneyUsage,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsageSubCategory?>> {
        val context = getContext(env)
        val userId = context.verifyUserSessionAndGetUserId()
        return getMoneyUsageFutureResult(
            context = context,
            env = env,
            userId = userId,
            moneyUsageId = moneyUsage.id,
        ).thenApplyAsync { futureResult ->
            val subCategoryId = futureResult.subCategoryId ?: return@thenApplyAsync null
            QlMoneyUsageSubCategory(
                id = subCategoryId,
            )
        }.toDataFetcher()
    }

    override fun linkedMail(
        moneyUsage: QlMoneyUsage,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<List<QlImportedMail>?>> {
        val context = getContext(env)
        val userId = context.verifyUserSessionAndGetUserId()

        return context.dataLoaders.moneyUsageAssociateByImportedMailDataLoader.get(env)
            .load(
                MoneyUsageAssociateByImportedMailDataLoaderDefine.Key(
                    userId = userId,
                    moneyUsageId = moneyUsage.id,
                ),
            ).thenApplyAsync { relationFuture ->
                val hoge = relationFuture ?: return@thenApplyAsync null
                hoge.mailIdList.map { id ->
                    QlImportedMail(id = id)
                }
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
