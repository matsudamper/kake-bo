package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.viewmodel.SplashScreenViewModel
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery
import net.matsudamper.money.frontend.graphql.ServerHostConfig

@Composable
internal fun SplashScreenContainer(
    navController: ScreenNavController,
) {
    val koin = LocalKoin.current
    val graphqlClient = koin.get<GraphqlClient>()
    LocalScopedObjectStore.current.putOrGet<SplashScreenViewModel>(Unit) {
        SplashScreenViewModel(
            scopedObjectFeature = it,
            navController = navController,
            graphqlQuery = GraphqlUserLoginQuery(
                graphqlClient = graphqlClient,
            ),
            serverHostConfig = koin.getOrNull<ServerHostConfig>(),
        )
    }
    Box(modifier = Modifier.fillMaxSize())
}
