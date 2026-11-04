package net.matsudamper.money.backend.graphql.resolver.importedmail

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.app.interfaces.MailFilterRepository
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailFilterCategoryConditionOperator
import net.matsudamper.money.backend.dataloader.ImportedMailCategoryFilterDataLoaderDefine
import net.matsudamper.money.backend.dataloader.ImportedMailCategoryFilterMatcherDataLoaderDefine
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.otelThenApplyAsync
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.element.UserId
import net.matsudamper.money.graphql.model.ImportedMailCategoryFilterResolver
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilter
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilterMatcher
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

        return CompletableFuture.allOf(future).otelThenApplyAsync {
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

        return CompletableFuture.allOf(future).otelThenApplyAsync {
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

        return CompletableFuture.allOf(future).otelThenApplyAsync {
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

        return CompletableFuture.allOf(future).otelThenApplyAsync {
            when (future.get()!!.operator) {
                ImportedMailFilterCategoryConditionOperator.AND -> QlImportedMailFilterCategoryConditionOperator.AND
                ImportedMailFilterCategoryConditionOperator.OR -> QlImportedMailFilterCategoryConditionOperator.OR
            }
        }.toDataFetcher()
    }

    override fun matchExpression(
        importedMailCategoryFilter: QlImportedMailCategoryFilter,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val future = getImportedMailCategoryFilterFuture(
            context = context,
            userId = userId,
            importedMailCategoryFilter = importedMailCategoryFilter,
            env = env,
        )

        return CompletableFuture.allOf(future).otelThenApplyAsync {
            future.get().matchExpression
        }.toDataFetcher()
    }

    override fun matchers(
        importedMailCategoryFilter: QlImportedMailCategoryFilter,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<List<QlImportedMailCategoryFilterMatcher>?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()

        val dataLoader = context.dataLoaders.importedMailCategoryFilterMatcherDataLoader.get(env)

        return CompletableFuture.allOf().otelThenApplyAsync {
            val result = context.diContainer.createMailFilterRepository()
                .getMatchers(
                    userId = userId,
                    filterId = importedMailCategoryFilter.id,
                ).onFailure {
                    it.printStackTrace()
                }.getOrNull() ?: return@otelThenApplyAsync null

            result.matchers.map { matcher ->
                dataLoader.prime(
                    ImportedMailCategoryFilterMatcherDataLoaderDefine.Key(
                        userId = userId,
                        matcherId = matcher.matcherId,
                    ),
                    matcher,
                )

                QlImportedMailCategoryFilterMatcher(
                    id = matcher.matcherId,
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
