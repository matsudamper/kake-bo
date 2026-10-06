package net.matsudamper.money.frontend.common.viewmodel.importedmail.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailScreenDeleteMailMutation
import net.matsudamper.money.frontend.graphql.ImportedMailScreenParseWithAiMutation
import net.matsudamper.money.frontend.graphql.ImportedMailScreenQuery
import net.matsudamper.money.frontend.graphql.fragment.ImportedMailScreenSuggestUsage
import net.matsudamper.money.frontend.graphql.type.ParseImportedMailWithAiError

public class ImportedMailScreenGraphqlApi(
    private val graphqlClient: GraphqlClient,
) {
    public suspend fun delete(id: ImportedMailId): Boolean {
        return withContext(Dispatchers.IO) {
            runCatching {
                graphqlClient.apolloClient
                    .mutation(ImportedMailScreenDeleteMailMutation(id = id))
                    .execute()
            }.onFailure {
                it.printStackTrace()
            }.fold(
                onSuccess = {
                    it.data?.userMutation?.deleteImportedMail == true
                },
                onFailure = { false },
            )
        }
    }

    public suspend fun parseWithAi(id: ImportedMailId): ParseWithAiResult {
        return withContext(Dispatchers.IO) {
            runCatching {
                graphqlClient.apolloClient
                    .mutation(ImportedMailScreenParseWithAiMutation(id = id))
                    .execute()
            }.onFailure {
                it.printStackTrace()
            }.fold(
                onSuccess = { response ->
                    val result = response.data?.userMutation?.parseImportedMailWithAi
                        ?: return@fold ParseWithAiResult.Failure(message = null)
                    when (result.error) {
                        null -> ParseWithAiResult.Success(
                            usages = result.usages.map { it.importedMailScreenSuggestUsage },
                        )

                        ParseImportedMailWithAiError.ApiKeyNotSet -> ParseWithAiResult.ApiKeyNotSet
                        ParseImportedMailWithAiError.MailNotFound -> ParseWithAiResult.MailNotFound
                        ParseImportedMailWithAiError.ParseFailed,
                        ParseImportedMailWithAiError.InternalServerError,
                        ParseImportedMailWithAiError.UNKNOWN__,
                        -> ParseWithAiResult.Failure(message = result.errorMessage)
                    }
                },
                onFailure = { ParseWithAiResult.Failure(message = null) },
            )
        }
    }

    public suspend fun get(id: ImportedMailId): Result<ApolloResponse<ImportedMailScreenQuery.Data>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                graphqlClient.apolloClient.query(
                    ImportedMailScreenQuery(
                        id = id,
                    ),
                )
                    .fetchPolicy(FetchPolicy.NetworkOnly)
                    .execute()
            }
        }
    }

    public sealed interface ParseWithAiResult {
        public data class Success(val usages: List<ImportedMailScreenSuggestUsage>) : ParseWithAiResult

        public data object ApiKeyNotSet : ParseWithAiResult

        public data object MailNotFound : ParseWithAiResult

        public data class Failure(val message: String?) : ParseWithAiResult
    }
}
