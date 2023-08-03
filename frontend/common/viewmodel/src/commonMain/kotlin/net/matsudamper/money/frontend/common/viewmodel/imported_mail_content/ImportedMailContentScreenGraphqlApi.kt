package net.matsudamper.money.frontend.common.viewmodel.imported_mail_content

import com.apollographql.apollo3.ApolloClient
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailContentScreenQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseCollector

public class ImportedMailContentScreenGraphqlApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {

    public fun get(
        id: ImportedMailId,
    ): ApolloResponseCollector<ImportedMailContentScreenQuery.Data> {
        return ApolloResponseCollector.create(
            apolloClient = apolloClient,
            query = ImportedMailContentScreenQuery(
                id = id,
            ),
        )
    }
}
