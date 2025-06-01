package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.feature.webauth.WebAuthModel
import net.matsudamper.money.frontend.common.ui.screen.login.LoginScreen
import net.matsudamper.money.frontend.common.ui.screen.login.LoginScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.LoginScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.login.LoginScreenApi
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery

@Composable
internal fun LoginScreenContainer(
    navController: ScreenNavController,
    globalEventSender: EventSender<GlobalEvent>,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel = LocalScopedObjectStore.current.putOrGet<LoginScreenViewModel>(Unit) {
        LoginScreenViewModel(
            scopedObjectFeature = it,
            navController = navController,
            graphqlQuery = GraphqlUserLoginQuery(
                graphqlClient = koin.get<GraphqlClient>(),
            ),
            globalEventSender = globalEventSender,
            screenApi = LoginScreenApi(
                graphqlClient = koin.get<GraphqlClient>(),
            ),
            webAuthModel = koin.get<WebAuthModel>(),
        )
    }
    val uiState: LoginScreenUiState = viewModel.uiStateFlow.collectAsState().value
    LoginScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = uiState,
        windowInsets = windowInsets,
    )
}
