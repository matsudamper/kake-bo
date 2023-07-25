package net.matsudamper.money.backend.graphql.resolver

import java.time.ZoneOffset
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.graphql.model.ImportedMailAttributesResolver
import net.matsudamper.money.graphql.model.QlImportedMail
import net.matsudamper.money.graphql.model.QlImportedMailAttributes
import net.matsudamper.money.graphql.model.QlImportedMailConnection
import net.matsudamper.money.graphql.model.QlImportedMailQuery
import net.matsudamper.money.graphql.model.QlImportedMailQueryFilter

public class ImportedMailAttributesResolverImpl : ImportedMailAttributesResolver {
    override fun count(importedMailAttributes: QlImportedMailAttributes, query: QlImportedMailQueryFilter, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<Int?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return CompletableFuture.supplyAsync {
            // TODO filter
            context.repositoryFactory.createDbMailRepository()
                .getCount(
                    userId = userId,
                )
        }.toDataFetcher()
    }

    override fun mails(importedMailAttributes: QlImportedMailAttributes, mailQuery: QlImportedMailQuery, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlImportedMailConnection>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return CompletableFuture.supplyAsync {
            val mails = context.repositoryFactory.createDbMailRepository()
                .getMails(
                    userId = userId,
                    size = 10,
                )
            QlImportedMailConnection(
                cursor = null, // TODO
                nodes = mails.map {
                    QlImportedMail(
                        id = it.id,
                        plain = it.plain,
                        html = it.html,
                        from = it.from,
                        subject = it.subject,
                        time = it.dateTime,
                    )
                },
            )
        }.toDataFetcher()
    }
}
