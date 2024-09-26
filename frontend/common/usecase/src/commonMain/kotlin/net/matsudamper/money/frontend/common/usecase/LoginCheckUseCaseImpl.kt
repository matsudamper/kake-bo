package net.matsudamper.money.frontend.common.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery

public interface LoginCheckUseCase {
    public suspend fun check(): Boolean
}

public class LoginCheckUseCaseImpl(
    private val ioDispatcher: CoroutineDispatcher,
    private val graphqlQuery: GraphqlUserLoginQuery,
    private val eventListener: EventListener,
) : LoginCheckUseCase {
    override suspend fun check(): Boolean {
        return runCatching {
            withContext(ioDispatcher) {
                graphqlQuery.isLoggedIn()
            }
        }.onFailure { e ->
            eventListener.error("${e.message}")
        }.onSuccess { isLoggedIn ->
            if (isLoggedIn.not()) {
                eventListener.logout()
            }
        }.fold(onSuccess = { isLoggedIn -> isLoggedIn }, onFailure = { false })
    }

    public interface EventListener {
        public fun error(message: String)
        public fun logout()
    }
}
