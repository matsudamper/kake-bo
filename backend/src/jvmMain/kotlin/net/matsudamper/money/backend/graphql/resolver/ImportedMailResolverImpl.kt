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
import java.time.LocalDateTime

class ImportedMailResolverImpl : ImportedMailResolver {
    override fun subject(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment
    ): CompletionStage<DataFetcherResult<String>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
        val importedMailFuture = context.dataLoaders.importedMailDataLoader.get(env)
            .load(
                ImportedMailDataLoaderDefine.Key(
                    userId = userId,
                    importedMailId = importedMail.id
                )
            )
        return CompletableFuture.supplyAsync {
            importedMailFuture.get().subject
        }.toDataFetcher()
    }

    override fun from(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment
    ): CompletionStage<DataFetcherResult<String>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
        val importedMailFuture = context.dataLoaders.importedMailDataLoader.get(env)
            .load(
                ImportedMailDataLoaderDefine.Key(
                    userId = userId,
                    importedMailId = importedMail.id
                )
            )
        return CompletableFuture.supplyAsync {
            importedMailFuture.get().from
        }.toDataFetcher()
    }

    override fun plain(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment
    ): CompletionStage<DataFetcherResult<String?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
        val importedMailFuture = context.dataLoaders.importedMailDataLoader.get(env)
            .load(
                ImportedMailDataLoaderDefine.Key(
                    userId = userId,
                    importedMailId = importedMail.id
                )
            )
        return CompletableFuture.supplyAsync {
            importedMailFuture.get().plain
        }.toDataFetcher()
    }

    override fun html(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment
    ): CompletionStage<DataFetcherResult<String?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
        val importedMailFuture = context.dataLoaders.importedMailDataLoader.get(env)
            .load(
                ImportedMailDataLoaderDefine.Key(
                    userId = userId,
                    importedMailId = importedMail.id
                )
            )
        return CompletableFuture.supplyAsync {
            importedMailFuture.get().html
        }.toDataFetcher()
    }

    override fun time(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment
    ): CompletionStage<DataFetcherResult<LocalDateTime>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
        val importedMailFuture = context.dataLoaders.importedMailDataLoader.get(env)
            .load(
                ImportedMailDataLoaderDefine.Key(
                    userId = userId,
                    importedMailId = importedMail.id
                )
            )
        return CompletableFuture.supplyAsync {
            importedMailFuture.get().dateTime
        }.toDataFetcher()
    }

    override fun suggestUsages(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment
    ): CompletionStage<DataFetcherResult<List<QlSuggestMoneyUsage>>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        val mailLoader = context.dataLoaders.importedMailDataLoader.get(env).load(
            ImportedMailDataLoaderDefine.Key(
                userId = userId,
                importedMailId = importedMail.id,
            ),
        )
        return CompletableFuture.supplyAsync {
            val targetMail = mailLoader.get() ?: return@supplyAsync listOf()

            val results = MailMoneyUsageParser().parse(
                subject = targetMail.subject,
                from = targetMail.from,
                html = targetMail.html.orEmpty(),
                plain = targetMail.plain.orEmpty(),
                date = targetMail.dateTime,
            )

            results.map { result ->
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
            }
        }.toDataFetcher()
    }
}
