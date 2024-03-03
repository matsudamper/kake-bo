package net.matsudamper.money.backend.graphql.resolver.importedmail

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.app.interfaces.MailFilterRepository
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterConditionType
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterDatasourceType
import net.matsudamper.money.backend.dataloader.ImportedMailCategoryFilterConditionDataLoaderDefine
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.graphql.model.ImportedMailCategoryConditionResolver
import net.matsudamper.money.graphql.model.QlImportedMailCategoryCondition
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilterConditionType
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilterDataSourceType

class ImportedMailCategoryConditionResolverImpl : ImportedMailCategoryConditionResolver {
    override fun text(
        importedMailCategoryCondition: QlImportedMailCategoryCondition,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSessionAndGetUserId()

        val future =
            getFuture(
                env = env,
                importedMailCategoryCondition = importedMailCategoryCondition,
            )

        return CompletableFuture.allOf(future).thenApplyAsync {
            future.get()!!.text
        }.toDataFetcher()
    }

    override fun dataSourceType(
        importedMailCategoryCondition: QlImportedMailCategoryCondition,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlImportedMailCategoryFilterDataSourceType>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSessionAndGetUserId()

        val future =
            getFuture(
                env = env,
                importedMailCategoryCondition = importedMailCategoryCondition,
            )

        return CompletableFuture.allOf(future).thenApplyAsync {
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

    override fun conditionType(
        importedMailCategoryCondition: QlImportedMailCategoryCondition,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlImportedMailCategoryFilterConditionType>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSessionAndGetUserId()

        val future =
            getFuture(
                env = env,
                importedMailCategoryCondition = importedMailCategoryCondition,
            )

        return CompletableFuture.allOf(future).thenApplyAsync {
            when (future.get()!!.conditionType) {
                ImportedMailCategoryFilterConditionType.Include -> QlImportedMailCategoryFilterConditionType.Include
                ImportedMailCategoryFilterConditionType.NotInclude -> QlImportedMailCategoryFilterConditionType.NotInclude
                ImportedMailCategoryFilterConditionType.Equal -> QlImportedMailCategoryFilterConditionType.Equal
                ImportedMailCategoryFilterConditionType.NotEqual -> QlImportedMailCategoryFilterConditionType.NotEqual
            }
        }.toDataFetcher()
    }

    private fun getFuture(
        env: DataFetchingEnvironment,
        importedMailCategoryCondition: QlImportedMailCategoryCondition,
    ): CompletableFuture<MailFilterRepository.Condition> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        return context.dataLoaders.importedMailCategoryFilterConditionDataLoader.get(env)
            .load(
                ImportedMailCategoryFilterConditionDataLoaderDefine.Key(
                    userId = userId,
                    conditionId = importedMailCategoryCondition.id,
                ),
            )
    }
}
