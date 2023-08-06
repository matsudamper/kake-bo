package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.dataloader.ImportedMailCategoryFilterDataLoaderDefine
import net.matsudamper.money.backend.element.ImportedMailFilterCategoryConditionOperator
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.repository.MailFilterRepository
import net.matsudamper.money.graphql.model.ImportedMailCategoryFilterResolver
import net.matsudamper.money.graphql.model.QlImportedMailCategoryCondition
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilter
import net.matsudamper.money.graphql.model.QlImportedMailFilterCategoryConditionOperator
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategory

class ImportedMailCategoryFilterResolverImpl : ImportedMailCategoryFilterResolver {
    override fun title(importedMailCategoryFilter: QlImportedMailCategoryFilter, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<String>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
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

    override fun subCategory(importedMailCategoryFilter: QlImportedMailCategoryFilter, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlMoneyUsageSubCategory?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
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

    override fun orderNumber(importedMailCategoryFilter: QlImportedMailCategoryFilter, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<Int>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
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

    override fun operator(importedMailCategoryFilter: QlImportedMailCategoryFilter, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlImportedMailFilterCategoryConditionOperator>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
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

    override fun conditions(importedMailCategoryFilter: QlImportedMailCategoryFilter, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<List<QlImportedMailCategoryCondition>?>> {
        TODO("Not yet implemented")
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
