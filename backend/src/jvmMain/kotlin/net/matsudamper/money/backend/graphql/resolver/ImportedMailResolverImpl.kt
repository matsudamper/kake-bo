package net.matsudamper.money.backend.graphql.resolver

import java.time.LocalDateTime
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
import net.matsudamper.money.graphql.model.QlMoneyUsageSuggest

class ImportedMailResolverImpl : ImportedMailResolver {
    override fun subject(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return context.dataLoaders.importedMailDataLoader.get(env)
            .load(
                ImportedMailDataLoaderDefine.Key(
                    userId = userId,
                    importedMailId = importedMail.id,
                ),
            ).thenApplyAsync { importedMailFuture ->
                importedMailFuture.subject
            }.toDataFetcher()
    }

    override fun from(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return context.dataLoaders.importedMailDataLoader.get(env)
            .load(
                ImportedMailDataLoaderDefine.Key(
                    userId = userId,
                    importedMailId = importedMail.id,
                ),
            ).thenApplyAsync { importedMailFuture ->
                importedMailFuture.from
            }.toDataFetcher()
    }

    override fun plain(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
        return context.dataLoaders.importedMailDataLoader.get(env)
            .load(
                ImportedMailDataLoaderDefine.Key(
                    userId = userId,
                    importedMailId = importedMail.id,
                ),
            )
            .thenApplyAsync { importedMailFuture ->
                importedMailFuture.plain
            }.toDataFetcher()
    }

    override fun html(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
        return context.dataLoaders.importedMailDataLoader.get(env)
            .load(
                ImportedMailDataLoaderDefine.Key(
                    userId = userId,
                    importedMailId = importedMail.id,
                ),
            ).thenApplyAsync { importedMailFuture ->
                importedMailFuture.html
            }.toDataFetcher()
    }

    override fun dateTime(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<LocalDateTime>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return context.dataLoaders.importedMailDataLoader.get(env)
            .load(
                ImportedMailDataLoaderDefine.Key(
                    userId = userId,
                    importedMailId = importedMail.id,
                ),
            ).thenApplyAsync { importedMailFuture ->
                importedMailFuture.dateTime
            }.toDataFetcher()
    }

    override fun suggestUsages(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<List<QlMoneyUsageSuggest>>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()


        return context.dataLoaders.importedMailDataLoader.get(env).load(
            ImportedMailDataLoaderDefine.Key(
                userId = userId,
                importedMailId = importedMail.id,
            ),
        ).thenApplyAsync { mailLoader ->
            val targetMail = mailLoader ?: return@thenApplyAsync listOf()

            val results = MailMoneyUsageParser().parse(
                subject = targetMail.subject,
                from = targetMail.from,
                html = targetMail.html.orEmpty(),
                plain = targetMail.plain.orEmpty(),
                date = targetMail.dateTime,
            )
            results.map { result ->
                QlMoneyUsageSuggest(
                    title = result.title,
                    amount = result.price,
                    description = result.description,
                    dateTime = result.dateTime,
                    service = result.service.let {
                        QlMoneyUsageService(
                            id = it.toId(),
                            name = it.displayName,
                        )
                    },
                    subCategory = null, // todo
                )
            }
        }.toDataFetcher()
    }
}
