package net.matsudamper.money.backend.graphql.resolver.importedmail

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.repository.DbMailRepository
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.graphql.model.ImportedMailAttributesResolver
import net.matsudamper.money.graphql.model.QlImportedMail
import net.matsudamper.money.graphql.model.QlImportedMailAttributes
import net.matsudamper.money.graphql.model.QlImportedMailConnection
import net.matsudamper.money.graphql.model.QlImportedMailQuery
import net.matsudamper.money.graphql.model.QlImportedMailQueryFilter
import net.matsudamper.money.graphql.model.QlImportedMailSortKey
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

public class ImportedMailAttributesResolverImpl : ImportedMailAttributesResolver {
    override fun count(
        importedMailAttributes: QlImportedMailAttributes,
        query: QlImportedMailQueryFilter,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Int?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return CompletableFuture.supplyAsync {
            context.repositoryFactory.createDbMailRepository()
                .getCount(
                    userId = userId,
                    isLinked = query.isLinked,
                )
        }.toDataFetcher()
    }

    override fun mails(
        importedMailAttributes: QlImportedMailAttributes,
        query: QlImportedMailQuery,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlImportedMailConnection>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
        return CompletableFuture.supplyAsync {
            val cursor = query.cursor?.let { ImportedMailAttributesMailsQueryCursor.fromString(it) }
            val mailResult = context.repositoryFactory.createDbMailRepository()
                .getMails(
                    userId = userId,
                    size = query.size,
                    isLinked = query.filter.isLinked,
                    sortedKey = when (query.sortedBy) {
                        QlImportedMailSortKey.CREATED_DATETIME -> DbMailRepository.MailSortedKey.CREATE_DATETIME
                        QlImportedMailSortKey.DATETIME -> DbMailRepository.MailSortedKey.DATETIME
                    },
                    pagingInfo = cursor?.pagingInfo,
                    isAsc = query.isAsc,
                )

            QlImportedMailConnection(
                cursor = mailResult.pagingInfo?.let { pagingInfo ->
                    ImportedMailAttributesMailsQueryCursor(
                        pagingInfo,
                    ).toCursorString()
                },
                nodes = mailResult.mails.map {
                    QlImportedMail(
                        id = it,
                    )
                },
            )
        }.toDataFetcher()
    }

    override fun mail(
        importedMailAttributes: QlImportedMailAttributes,
        id: ImportedMailId,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlImportedMail?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSession()

        return CompletableFuture.supplyAsync {
            QlImportedMail(
                id = id,
            )
        }.toDataFetcher()
    }
}