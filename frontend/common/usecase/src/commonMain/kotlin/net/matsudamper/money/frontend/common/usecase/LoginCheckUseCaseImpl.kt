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
    /**
     * @return ログアウトさせた方が良い場合はfalseが返る
     */
    override suspend fun check(): Boolean {
        val forceLogout = withContext(Dispatchers.IO) {
            graphqlQuery.isLoggedIn()
        }.not()
        if (forceLogout) eventListener.logout()
        return forceLogout.not()
    }

    public interface EventListener {
        public fun error(message: String)
        public fun logout()
    }
}
