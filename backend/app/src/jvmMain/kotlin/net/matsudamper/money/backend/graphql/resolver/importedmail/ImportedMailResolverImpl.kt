package net.matsudamper.money.backend.graphql.resolver.importedmail

import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.dataloader.ImportedMailDataLoaderDefine
import net.matsudamper.money.backend.dataloader.MoneyUsageDataLoaderDefine
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.localcontext.MoneyUsageSuggestLocalContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.mail.parser.MailParser
import net.matsudamper.money.graphql.model.ImportedMailResolver
import net.matsudamper.money.graphql.model.QlImportedMail
import net.matsudamper.money.graphql.model.QlImportedMailForwardedInfo
import net.matsudamper.money.graphql.model.QlMoneyUsage
import net.matsudamper.money.graphql.model.QlMoneyUsageSuggest

class ImportedMailResolverImpl : ImportedMailResolver {
    private val DataFetchingEnvironment.typedContext: GraphQlContext get() = graphQlContext.get(GraphQlContext::class.java.name)

    override fun subject(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String>> {
        val context = env.typedContext
        val userId = context.verifyUserSessionAndGetUserId()

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
        val context = env.typedContext
        val userId = context.verifyUserSessionAndGetUserId()

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
        val context = env.typedContext
        val userId = context.verifyUserSessionAndGetUserId()
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

    override fun hasPlain(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Boolean>> {
        val context = env.typedContext
        val userId = context.verifyUserSessionAndGetUserId()
        return context.dataLoaders.importedMailDataLoader.get(env)
            .load(
                ImportedMailDataLoaderDefine.Key(
                    userId = userId,
                    importedMailId = importedMail.id,
                ),
            ).thenApplyAsync { importedMailFuture ->
                importedMailFuture.plain != null
            }.toDataFetcher()
    }

    override fun html(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String?>> {
        val context = env.typedContext
        val userId = context.verifyUserSessionAndGetUserId()
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

    override fun hasHtml(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Boolean>> {
        val context = env.typedContext
        val userId = context.verifyUserSessionAndGetUserId()
        return context.dataLoaders.importedMailDataLoader.get(env)
            .load(
                ImportedMailDataLoaderDefine.Key(
                    userId = userId,
                    importedMailId = importedMail.id,
                ),
            ).thenApplyAsync { importedMailFuture ->
                importedMailFuture.html != null
            }.toDataFetcher()
    }

    override fun dateTime(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<LocalDateTime>> {
        val context = env.typedContext
        val userId = context.verifyUserSessionAndGetUserId()

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
        val context = env.typedContext
        val userId = context.verifyUserSessionAndGetUserId()

        return context.dataLoaders.importedMailDataLoader.get(env).load(
            ImportedMailDataLoaderDefine.Key(
                userId = userId,
                importedMailId = importedMail.id,
            ),
        ).thenApplyAsync { mailLoader ->
            val targetMail = mailLoader ?: return@thenApplyAsync run {
                DataFetcherResult.newResult<List<QlMoneyUsageSuggest>>()
                    .data(listOf())
                    .build()
            }

            val results = MailParser.parseUsage(
                subject = targetMail.subject,
                from = targetMail.from,
                html = targetMail.html.orEmpty(),
                plain = targetMail.plain.orEmpty(),
                date = targetMail.dateTime,
            )

            DataFetcherResult.newResult<List<QlMoneyUsageSuggest>>()
                .data(
                    results.map { result ->
                        QlMoneyUsageSuggest(
                            title = result.title,
                            amount = result.price,
                            description = result.description,
                            dateTime = result.dateTime,
                            serviceName = result.service.displayName,
                        )
                    },
                )
                .localContext(
                    MoneyUsageSuggestLocalContext(
                        importedMailId = targetMail.id,
                    ),
                )
                .build()
        }
    }

    override fun usages(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<List<QlMoneyUsage>>> {
        val context = env.typedContext
        val userId = context.verifyUserSessionAndGetUserId()

        val moneyUsageLoader = context.dataLoaders.moneyUsageDataLoader.get(env)

        return CompletableFuture.allOf().thenApplyAsync {
            val result = context.diContainer.createMoneyUsageRepository()
                .getMails(
                    userId = userId,
                    importedMailId = importedMail.id,
                )

            result.onSuccess { usages ->
                usages.forEach { usage ->
                    moneyUsageLoader.prime(
                        MoneyUsageDataLoaderDefine.Key(
                            userId = userId,
                            moneyUsageId = usage.id,
                        ),
                        MoneyUsageDataLoaderDefine.MoneyUsage(
                            id = usage.id,
                            title = usage.title,
                            amount = usage.amount,
                            description = usage.description,
                            userId = usage.userId,
                            subCategoryId = usage.subCategoryId,
                            date = usage.date,
                        ),
                    )
                }
            }.onFailure {
                it.printStackTrace()
            }

            result.fold(
                onSuccess = { usages ->
                    usages.map { usage ->
                        QlMoneyUsage(
                            id = usage.id,
                        )
                    }
                },
                onFailure = {
                    listOf()
                },
            )
        }.toDataFetcher()
    }

    override fun forwardedInfo(
        importedMail: QlImportedMail,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlImportedMailForwardedInfo?>> {
        val context = env.typedContext
        val userId = context.verifyUserSessionAndGetUserId()

        return context.dataLoaders.importedMailDataLoader.get(env)
            .load(
                ImportedMailDataLoaderDefine.Key(
                    userId = userId,
                    importedMailId = importedMail.id,
                ),
            ).thenApplyAsync { importedMailFuture ->
                val parsed = MailParser
                    .forwardedInfo(importedMailFuture.plain ?: return@thenApplyAsync null)
                    ?: return@thenApplyAsync null
                QlImportedMailForwardedInfo(
                    from = parsed.from,
                    subject = parsed.subject,
                    dateTime = parsed.dateTime,
                )
            }.toDataFetcher()
    }
}
