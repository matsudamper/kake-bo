package net.matsudamper.money.frontend.common.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery

public interface LoginCheckUseCase {
    public suspend fun check(): Boolean
}

public class LoginCheckUseCaseImpl(
    private val graphqlQuery: GraphqlUserLoginQuery,
    private val eventListener: EventListener,
) : LoginCheckUseCase {
    override suspend fun check(): Boolean {
        return runCatching {
            withContext(Dispatchers.IO) {
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
