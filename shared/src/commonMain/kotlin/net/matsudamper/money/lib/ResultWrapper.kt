package net.matsudamper.money.lib

public sealed interface ResultWrapper<T> {
    public class Success<T>(public val value: T) : ResultWrapper<T>

    public class Failure<T>(public val throwable: Throwable) : ResultWrapper<T>

    public companion object {
        public fun <T> success(value: T): ResultWrapper<T> = Success(value)

        public fun <T> failure(throwable: Throwable): ResultWrapper<T> = Failure(throwable)
    }
}
