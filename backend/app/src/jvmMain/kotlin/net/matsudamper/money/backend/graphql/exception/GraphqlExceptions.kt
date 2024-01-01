package net.matsudamper.money.backend.graphql.exception

sealed class GraphqlExceptions(message: String, e: Throwable?) : Exception(message, e) {
    class BadRequest(message: String, e: Throwable? = null) : GraphqlExceptions(message, e)
}
