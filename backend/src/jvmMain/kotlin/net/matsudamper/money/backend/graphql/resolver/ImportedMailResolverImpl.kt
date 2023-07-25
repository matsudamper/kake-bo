package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.dataloader.ImportedMailDataLoaderDefine
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.mail.parser.MailMoneyUsageParser
import net.matsudamper.money.graphql.model.ImportedMailResolver
import net.matsudamper.money.graphql.model.QlImportedMail
import net.matsudamper.money.graphql.model.QlMoneyUsageService
import net.matsudamper.money.graphql.model.QlSuggestMoneyUsage

class ImportedMailResolverImpl : ImportedMailResolver {
    override fun suggestUsage(importedMail: QlImportedMail, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlSuggestMoneyUsage?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        val mailLoader = context.dataLoaders.importedMailDataLoader.get(env).load(
            ImportedMailDataLoaderDefine.Key(
                userId = userId,
                importedMailId = importedMail.id,
            )
        )
        return CompletableFuture.supplyAsync {
            val targetMail = mailLoader.get() ?: return@supplyAsync null

            val result = MailMoneyUsageParser().parse(
                subject = targetMail.subject,
                from = targetMail.from,
                html = targetMail.html.orEmpty(),
                plain = targetMail.plain.orEmpty(),
            ) ?: return@supplyAsync null

            QlSuggestMoneyUsage(
                title = result.title,
                price = result.price,
                description = result.description,
                date = result.dateTime,
                service = result.service.let {
                    QlMoneyUsageService(
                        id = it.toId(),
                        name = it.displayName,
                    )
                },
                type = null, // TODO
            )
        }.toDataFetcher()
    }
}
