package net.matsudamper.money.frontend.common.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery

public sealed interface LoginCheckResult {
    public data object Success : LoginCheckResult
    public data object NeedLogin : LoginCheckResult
    public data object ServerError : LoginCheckResult
}

public interface LoginCheckUseCase {
    public suspend fun check(): LoginCheckResult
}

public class LoginCheckUseCaseImpl(
    private val graphqlQuery: GraphqlUserLoginQuery,
    private val eventListener: EventListener,
) : LoginCheckUseCase {
    override suspend fun check(): LoginCheckResult {
        return when (withContext(Dispatchers.IO) { graphqlQuery.isLoggedIn() }) {
            GraphqlUserLoginQuery.IsLoggedInResult.LoggedIn -> LoginCheckResult.Success
            GraphqlUserLoginQuery.IsLoggedInResult.NotLoggedIn -> {
                eventListener.logout()
                LoginCheckResult.NeedLogin
            }
            GraphqlUserLoginQuery.IsLoggedInResult.ServerError -> {
                eventListener.serverError()
                LoginCheckResult.ServerError
            }
        }
    }

    public interface EventListener {
        public fun error(message: String)
        public fun logout()
        public fun serverError()
    }
}
