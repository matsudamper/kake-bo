package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.base.nav.user.JsScreenNavController
import net.matsudamper.money.frontend.common.ui.screen.login.LoginScreen
import net.matsudamper.money.frontend.common.ui.screen.login.LoginScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.LoginScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.login.LoginScreenApi
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery
import net.matsudamper.money.ui.root.viewmodel.provideViewModel

@Composable
internal fun LoginScreenContainer(
    navController: JsScreenNavController,
    globalEventSender: EventSender<GlobalEvent>,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel = provideViewModel {
        LoginScreenViewModel(
            viewModelFeature = it,
            navController = navController,
            graphqlQuery = GraphqlUserLoginQuery(
                graphqlClient = koin.get(),
            ),
            globalEventSender = globalEventSender,
            screenApi = LoginScreenApi(
                graphqlClient = koin.get(),
            ),
            webAuthModel = koin.get(),
        )
    }
    val uiState: LoginScreenUiState = viewModel.uiStateFlow.collectAsState().value
    LoginScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = uiState,
        windowInsets = windowInsets,
    )
}
