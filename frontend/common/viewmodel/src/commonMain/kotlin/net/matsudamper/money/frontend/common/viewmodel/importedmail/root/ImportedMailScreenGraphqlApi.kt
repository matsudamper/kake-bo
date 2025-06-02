package net.matsudamper.money.frontend.common.viewmodel.importedmail.root

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailScreenDeleteMailMutation
import net.matsudamper.money.frontend.graphql.ImportedMailScreenQuery

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
}
