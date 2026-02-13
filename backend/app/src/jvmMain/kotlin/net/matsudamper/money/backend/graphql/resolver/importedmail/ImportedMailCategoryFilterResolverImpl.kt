package net.matsudamper.money.backend.graphql.resolver.importedmail

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.app.interfaces.MailFilterRepository
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailFilterCategoryConditionOperator
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.backend.dataloader.ImportedMailCategoryFilterConditionDataLoaderDefine
import net.matsudamper.money.backend.dataloader.ImportedMailCategoryFilterDataLoaderDefine
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.element.UserId
import net.matsudamper.money.graphql.model.ImportedMailCategoryFilterResolver
import net.matsudamper.money.graphql.model.QlImportedMailCategoryCondition
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilter
import net.matsudamper.money.graphql.model.QlImportedMailFilterCategoryConditionOperator
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategory

class ImportedMailCategoryFilterResolverImpl : ImportedMailCategoryFilterResolver {
    override fun title(
        importedMailCategoryFilter: QlImportedMailCategoryFilter,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val future = getImportedMailCategoryFilterFuture(
            context = context,
            userId = userId,
            importedMailCategoryFilter = importedMailCategoryFilter,
            env = env,
        )

        return CompletableFuture.allOf(future).thenApplyAsync {
            future.get()!!.title
        }.toDataFetcher()
    }

    override fun subCategory(
        importedMailCategoryFilter: QlImportedMailCategoryFilter,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsageSubCategory?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val future = getImportedMailCategoryFilterFuture(
            context = context,
            userId = userId,
            importedMailCategoryFilter = importedMailCategoryFilter,
            env = env,
        )

        return CompletableFuture.allOf(future).thenApplyAsync {
            future.get()!!.moneyUsageSubCategoryId?.let {
                QlMoneyUsageSubCategory(
                    id = it,
                )
            }
        }.toDataFetcher()
    }

    override fun orderNumber(
        importedMailCategoryFilter: QlImportedMailCategoryFilter,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Int>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val future = getImportedMailCategoryFilterFuture(
            context = context,
            userId = userId,
            importedMailCategoryFilter = importedMailCategoryFilter,
            env = env,
        )

        return CompletableFuture.allOf(future).thenApplyAsync {
            future.get()!!.orderNumber
        }.toDataFetcher()
    }

    override fun operator(
        importedMailCategoryFilter: QlImportedMailCategoryFilter,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlImportedMailFilterCategoryConditionOperator>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val future = getImportedMailCategoryFilterFuture(
            context = context,
            userId = userId,
            importedMailCategoryFilter = importedMailCategoryFilter,
            env = env,
        )

        return CompletableFuture.allOf(future).thenApplyAsync {
            when (future.get()!!.operator) {
                ImportedMailFilterCategoryConditionOperator.AND -> QlImportedMailFilterCategoryConditionOperator.AND
                ImportedMailFilterCategoryConditionOperator.OR -> QlImportedMailFilterCategoryConditionOperator.OR
            }
        }.toDataFetcher()
    }

    override fun conditions(
        importedMailCategoryFilter: QlImportedMailCategoryFilter,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<List<QlImportedMailCategoryCondition>?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()

        val dataLoader = context.dataLoaders.importedMailCategoryFilterConditionDataLoader.get(env)

        return CompletableFuture.allOf().thenApplyAsync {
            val result = context.diContainer.createMailFilterRepository()
                .getConditions(
                    userId = userId,
                    filterId = importedMailCategoryFilter.id,
                ).onFailure {
                    TraceLogger.impl().noticeThrowable(it, mapOf(), true)
                }.getOrNull() ?: return@thenApplyAsync null

            result.conditions.map { condition ->
                dataLoader.prime(
                    ImportedMailCategoryFilterConditionDataLoaderDefine.Key(
                        userId = userId,
                        conditionId = condition.conditionId,
                    ),
                    condition,
                )

                QlImportedMailCategoryCondition(
                    id = condition.conditionId,
                )
            }
        }.toDataFetcher()
    }

    private fun getImportedMailCategoryFilterFuture(
        context: GraphQlContext,
        importedMailCategoryFilter: QlImportedMailCategoryFilter,
        userId: UserId,
        env: DataFetchingEnvironment,
    ): CompletableFuture<MailFilterRepository.MailFilter> {
        val dataLoader = context.dataLoaders.importedMailCategoryFilterDataLoader.get(env)
        return dataLoader.load(
            ImportedMailCategoryFilterDataLoaderDefine.Key(
                userId = userId,
                categoryFilterId = importedMailCategoryFilter.id,
            ),
        )
    }
}
