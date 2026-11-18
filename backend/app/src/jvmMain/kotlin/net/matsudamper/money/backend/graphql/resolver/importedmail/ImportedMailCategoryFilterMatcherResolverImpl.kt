package net.matsudamper.money.backend.graphql.resolver.importedmail

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.app.interfaces.MailFilterRepository
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterDatasourceType
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterMatcherType
import net.matsudamper.money.backend.dataloader.ImportedMailCategoryFilterMatcherDataLoaderDefine
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.otelThenApplyAsync
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.graphql.model.ImportedMailCategoryFilterMatcherResolver
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilterDataSourceType
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilterMatcher
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilterMatcherType

class ImportedMailCategoryFilterMatcherResolverImpl : ImportedMailCategoryFilterMatcherResolver {
    override fun matcherKey(
        importedMailCategoryFilterMatcher: QlImportedMailCategoryFilterMatcher,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSessionAndGetUserId()

        val future = getFuture(
            env = env,
            importedMailCategoryFilterMatcher = importedMailCategoryFilterMatcher,
        )

        return CompletableFuture.allOf(future).otelThenApplyAsync {
            future.get().matcherKey
        }.toDataFetcher()
    }

    override fun text(
        importedMailCategoryFilterMatcher: QlImportedMailCategoryFilterMatcher,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSessionAndGetUserId()

        val future = getFuture(
            env = env,
            importedMailCategoryFilterMatcher = importedMailCategoryFilterMatcher,
        )

        return CompletableFuture.allOf(future).otelThenApplyAsync {
            future.get()!!.text
        }.toDataFetcher()
    }

    override fun dataSourceType(
        importedMailCategoryFilterMatcher: QlImportedMailCategoryFilterMatcher,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlImportedMailCategoryFilterDataSourceType>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSessionAndGetUserId()

        val future = getFuture(
            env = env,
            importedMailCategoryFilterMatcher = importedMailCategoryFilterMatcher,
        )

        return CompletableFuture.allOf(future).otelThenApplyAsync {
            when (future.get().dataSourceType) {
                ImportedMailCategoryFilterDatasourceType.MailTitle -> QlImportedMailCategoryFilterDataSourceType.MailTitle
                ImportedMailCategoryFilterDatasourceType.MailFrom -> QlImportedMailCategoryFilterDataSourceType.MailFrom
                ImportedMailCategoryFilterDatasourceType.MailHTML -> QlImportedMailCategoryFilterDataSourceType.MailHtml
                ImportedMailCategoryFilterDatasourceType.Title -> QlImportedMailCategoryFilterDataSourceType.Title
                ImportedMailCategoryFilterDatasourceType.ServiceName -> QlImportedMailCategoryFilterDataSourceType.ServiceName
                ImportedMailCategoryFilterDatasourceType.MailPlain -> QlImportedMailCategoryFilterDataSourceType.MailPlain
            }
        }.toDataFetcher()
    }

    override fun matcherType(
        importedMailCategoryFilterMatcher: QlImportedMailCategoryFilterMatcher,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlImportedMailCategoryFilterMatcherType>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSessionAndGetUserId()

        val future = getFuture(
            env = env,
            importedMailCategoryFilterMatcher = importedMailCategoryFilterMatcher,
        )

        return CompletableFuture.allOf(future).otelThenApplyAsync {
            when (future.get()!!.matcherType) {
                ImportedMailCategoryFilterMatcherType.Include -> QlImportedMailCategoryFilterMatcherType.Include
                ImportedMailCategoryFilterMatcherType.NotInclude -> QlImportedMailCategoryFilterMatcherType.NotInclude
                ImportedMailCategoryFilterMatcherType.Equal -> QlImportedMailCategoryFilterMatcherType.Equal
                ImportedMailCategoryFilterMatcherType.NotEqual -> QlImportedMailCategoryFilterMatcherType.NotEqual
            }
        }.toDataFetcher()
    }

    private fun getFuture(
        env: DataFetchingEnvironment,
        importedMailCategoryFilterMatcher: QlImportedMailCategoryFilterMatcher,
    ): CompletableFuture<MailFilterRepository.Matcher> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        return context.dataLoaders.importedMailCategoryFilterMatcherDataLoader.get(env)
            .load(
                ImportedMailCategoryFilterMatcherDataLoaderDefine.Key(
                    userId = userId,
                    matcherId = importedMailCategoryFilterMatcher.id,
                ),
            )
    }
}
