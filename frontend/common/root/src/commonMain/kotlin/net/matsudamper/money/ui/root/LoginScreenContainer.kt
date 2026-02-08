package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import net.matsudamper.money.frontend.graphql.ServerHostConfig

@Composable
internal fun LoginScreenContainer(
    navController: ScreenNavController,
    globalEventSender: EventSender<GlobalEvent>,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val graphqlClient = koin.get<GraphqlClient>()
    val viewModel = LocalScopedObjectStore.current.putOrGet<LoginScreenViewModel>(Unit) {
        LoginScreenViewModel(
            scopedObjectFeature = it,
            navController = navController,
            graphqlQuery = GraphqlUserLoginQuery(
                graphqlClient = graphqlClient,
            ),
            globalEventSender = globalEventSender,
            screenApi = LoginScreenApi(
                graphqlClient = graphqlClient,
            ),
            webAuthModel = koin.get<WebAuthModel>(),
            graphqlClient = graphqlClient,
            serverHostConfig = koin.getOrNull<ServerHostConfig>(),
        )
    }
    val uiState: LoginScreenUiState = viewModel.uiStateFlow.collectAsState().value
    LoginScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = uiState,
        windowInsets = windowInsets,
    )
}
