package net.matsudamper.money.frontend.common.viewmodel

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery

public class LoginCheckUseCase(
    private val ioDispatcher: CoroutineDispatcher,
    private val navController: ScreenNavController<ScreenStructure>,
    private val globalEventSender: EventSender<GlobalEvent>,
    private val graphqlQuery: GraphqlUserLoginQuery,
) {
    public suspend fun check(): Boolean {
        return runCatching {
            withContext(ioDispatcher) {
                graphqlQuery.isLoggedIn()
            }
        }.onFailure { e ->
            globalEventSender.send {
                it.showSnackBar("${e.message}")
            }
        }.onSuccess { isLoggedIn ->
            if (isLoggedIn.not()) {
                navController.navigate(ScreenStructure.Login)
            }
        }.fold(onSuccess = { isLoggedIn -> isLoggedIn }, onFailure = { false })
    }
}
