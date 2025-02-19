package net.matsudamper.money.frontend.common.viewmodel.importedmail.root

import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailScreenDeleteMailMutation
import net.matsudamper.money.frontend.graphql.ImportedMailScreenQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseCollector

public class ImportedMailScreenGraphqlApi(
    private val graphqlClient: GraphqlClient,
) {
    public suspend fun delete(id: ImportedMailId): Boolean {
        return runCatching {
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

    public fun get(id: ImportedMailId): ApolloResponseCollector<ImportedMailScreenQuery.Data> {
        return ApolloResponseCollector.create(
            apolloClient = graphqlClient.apolloClient,
            query = ImportedMailScreenQuery(
                id = id,
            ),
        )
    }
}
