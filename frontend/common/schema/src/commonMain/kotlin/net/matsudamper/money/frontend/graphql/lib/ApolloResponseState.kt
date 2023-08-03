package net.matsudamper.money.frontend.graphql.lib

public sealed interface ApolloResponseState<T> {
    public class Loading<T> : ApolloResponseState<T>
    public class Success<T>(public val value: T) : ApolloResponseState<T>
    public class Failure<T>(public val throwable: Throwable) : ApolloResponseState<T>

    public companion object {
        public fun <T> loading(): ApolloResponseState<T> = Loading()
        public fun <T> success(value: T): ApolloResponseState<T> = Success(value)
        public fun <T> failure(throwable: Throwable): ApolloResponseState<T> = Failure(throwable)
    }
}
