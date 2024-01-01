package net.matsudamper.money.backend.graphql

sealed class GraphqlMoneyException(
    message: String = "",
    exception: Throwable? = null,
) : Throwable(message, exception) {
    class SessionNotVerify(
        exception: Throwable? = null,
    ) : GraphqlMoneyException(exception = exception)
}
