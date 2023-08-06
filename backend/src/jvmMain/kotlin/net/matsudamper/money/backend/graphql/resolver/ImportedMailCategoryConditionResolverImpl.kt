package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.graphql.model.ImportedMailCategoryConditionResolver
import net.matsudamper.money.graphql.model.QlImportedMailCategoryCondition
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilterConditionType
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilterDataSourceType

class ImportedMailCategoryConditionResolverImpl: ImportedMailCategoryConditionResolver {
    override fun text(importedMailCategoryCondition: QlImportedMailCategoryCondition, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<String>> {
        TODO("Not yet implemented")
    }

    override fun dataSourceType(
        importedMailCategoryCondition: QlImportedMailCategoryCondition,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlImportedMailCategoryFilterDataSourceType>> {
        TODO("Not yet implemented")
    }

    override fun conditionType(
        importedMailCategoryCondition: QlImportedMailCategoryCondition,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlImportedMailCategoryFilterConditionType>> {
        TODO("Not yet implemented")
    }
}
