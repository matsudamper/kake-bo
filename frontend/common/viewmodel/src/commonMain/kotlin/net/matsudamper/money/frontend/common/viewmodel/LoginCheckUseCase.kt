package net.matsudamper.money.frontend.common.viewmodel

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import MailScreenUiState
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery
import net.matsudamper.money.frontend.common.base.Screen
import net.matsudamper.money.frontend.common.base.ScreenNavController
import net.matsudamper.money.frontend.common.base.immutableListOf
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.graphql.GetMailQuery
import net.matsudamper.money.frontend.graphql.MailImportScreenGraphqlApi

public class LoginCheckUseCase(
    private val ioDispatcher: CoroutineDispatcher,
    private val navController: ScreenNavController,
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
                navController.navigate(Screen.Login)
            }
        }.fold(onSuccess = { isLoggedIn -> isLoggedIn }, onFailure = { false })
    }
}