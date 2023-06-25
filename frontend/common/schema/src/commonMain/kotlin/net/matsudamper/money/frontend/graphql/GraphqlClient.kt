package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo3.ApolloClient

object GraphqlClient {
    val apolloClient = ApolloClient.Builder()
        .serverUrl("${serverProtocol}//${serverHost}/query")
        .build()
}