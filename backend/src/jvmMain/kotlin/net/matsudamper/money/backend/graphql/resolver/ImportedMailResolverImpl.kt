package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.graphql.model.ImportedMailResolver
import net.matsudamper.money.graphql.model.QlImportedMail
import net.matsudamper.money.graphql.model.QlSuggestMoneyUsage

class ImportedMailResolverImpl: ImportedMailResolver {
    override fun suggestUsage(importedMail: QlImportedMail, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlSuggestMoneyUsage?>> {
        TODO("Not yet implemented")
    }
}