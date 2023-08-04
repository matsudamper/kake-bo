package net.matsudamper.money.frontend.common.viewmodel.importedmail

import com.apollographql.apollo3.ApolloClient
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailScreenQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseCollector

public class ImportedMailScreenGraphqlApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {

    public fun get(
        id: ImportedMailId,
        debug: String,
    ): ApolloResponseCollector<ImportedMailScreenQuery.Data> {
        return ApolloResponseCollector.create(
            apolloClient = apolloClient,
            query = ImportedMailScreenQuery(
                id = id,
            ),
            debug = debug,
        )
    }
}
