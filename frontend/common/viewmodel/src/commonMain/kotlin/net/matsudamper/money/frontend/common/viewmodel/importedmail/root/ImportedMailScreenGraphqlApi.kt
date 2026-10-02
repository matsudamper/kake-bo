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
import net.matsudamper.money.frontend.graphql.ImportedMailScreenQuery
import net.matsudamper.money.frontend.graphql.ImportedMailScreenStartAiParseMutation
import net.matsudamper.money.frontend.graphql.type.StartImportedMailAiParseError

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

    public suspend fun startAiParse(id: ImportedMailId): StartAiParseResult {
        return withContext(Dispatchers.IO) {
            runCatching {
                graphqlClient.apolloClient
                    .mutation(ImportedMailScreenStartAiParseMutation(id = id))
                    .execute()
            }.onFailure {
                it.printStackTrace()
            }.fold(
                onSuccess = { response ->
                    val result = response.data?.userMutation?.startImportedMailAiParse
                    when {
                        result == null -> StartAiParseResult.Failure
                        result.isSuccess -> StartAiParseResult.Success
                        else -> when (result.error) {
                            StartImportedMailAiParseError.ApiKeyNotSet -> StartAiParseResult.ApiKeyNotSet
                            StartImportedMailAiParseError.AlreadyRunning -> StartAiParseResult.AlreadyRunning
                            StartImportedMailAiParseError.MailNotFound,
                            StartImportedMailAiParseError.InternalServerError,
                            StartImportedMailAiParseError.UNKNOWN__,
                            null,
                            -> StartAiParseResult.Failure
                        }
                    }
                },
                onFailure = { StartAiParseResult.Failure },
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

    public sealed interface StartAiParseResult {
        public data object Success : StartAiParseResult

        public data object ApiKeyNotSet : StartAiParseResult

        public data object AlreadyRunning : StartAiParseResult

        public data object Failure : StartAiParseResult
    }
}
