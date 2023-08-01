package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.lib.CursorParser
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.graphql.model.ImportedMailAttributesResolver
import net.matsudamper.money.graphql.model.QlImportedMail
import net.matsudamper.money.graphql.model.QlImportedMailAttributes
import net.matsudamper.money.graphql.model.QlImportedMailConnection
import net.matsudamper.money.graphql.model.QlImportedMailQuery
import net.matsudamper.money.graphql.model.QlImportedMailQueryFilter

public class ImportedMailAttributesResolverImpl : ImportedMailAttributesResolver {
    override fun count(
        importedMailAttributes: QlImportedMailAttributes,
        query: QlImportedMailQueryFilter,
        env: DataFetchingEnvironment
    ): CompletionStage<DataFetcherResult<Int?>> {
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

    override fun mails(
        importedMailAttributes: QlImportedMailAttributes,
        query: QlImportedMailQuery,
        env: DataFetchingEnvironment
    ): CompletionStage<DataFetcherResult<QlImportedMailConnection>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return CompletableFuture.supplyAsync {
            val cursor = query.cursor?.let { ImportedMailAttributesMailsQueryCursor.fromString(it) }
            val mails = context.repositoryFactory.createDbMailRepository()
                .getMails(
                    userId = userId,
                    size = query.size,
                    isLinked = query.filter.isLinked,
                    lastMailId = cursor?.lastMailId,
                    isAsc = true,
                )

            QlImportedMailConnection(
                cursor = mails.lastOrNull()?.let {
                    ImportedMailAttributesMailsQueryCursor(
                        lastMailId = it,
                    ).toCursorString()
                },
                nodes = mails.map {
                    QlImportedMail(
                        id = it,
                    )
                },
            )
        }.toDataFetcher()
    }
}

private data class ImportedMailAttributesMailsQueryCursor(
    val lastMailId: ImportedMailId,
) {
    fun toCursorString(): String {
        return CursorParser.createToString(
            mapOf(
                LAST_MAIL_ID_KEY to lastMailId.id.toString(),
            ),
        )
    }

    companion object {
        private const val LAST_MAIL_ID_KEY = "lastMailId"
        fun fromString(value: String): ImportedMailAttributesMailsQueryCursor {
            return ImportedMailAttributesMailsQueryCursor(
                lastMailId = ImportedMailId(
                    id = CursorParser.parseFromString(value)[LAST_MAIL_ID_KEY]!!.toInt(),
                ),
            )
        }
    }
}
