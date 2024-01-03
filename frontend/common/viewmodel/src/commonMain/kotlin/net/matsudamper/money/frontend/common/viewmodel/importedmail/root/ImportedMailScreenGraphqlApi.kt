package net.matsudamper.money.frontend.common.viewmodel.importedmail.root

import com.apollographql.apollo3.ApolloClient
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailScreenDeleteMailMutation
import net.matsudamper.money.frontend.graphql.ImportedMailScreenQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseCollector

public class ImportedMailScreenGraphqlApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    public suspend fun delete(
        id: ImportedMailId,
    ): Boolean {
        return runCatching {
            apolloClient
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

    public fun get(
        id: ImportedMailId,
    ): ApolloResponseCollector<ImportedMailScreenQuery.Data> {
        return ApolloResponseCollector.create(
            apolloClient = apolloClient,
            query = ImportedMailScreenQuery(
                id = id,
            ),
        )
    }
}
