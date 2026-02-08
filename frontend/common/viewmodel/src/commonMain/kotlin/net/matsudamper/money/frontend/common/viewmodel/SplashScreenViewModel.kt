package net.matsudamper.money.frontend.common.viewmodel

import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery
import net.matsudamper.money.frontend.graphql.ServerHostConfig

public class SplashScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val navController: ScreenNavController,
    private val graphqlQuery: GraphqlUserLoginQuery,
    private val serverHostConfig: ServerHostConfig?,
) : CommonViewModel(scopedObjectFeature) {
    init {
        viewModelScope.launch {
            val hasHost = serverHostConfig != null &&
                (serverHostConfig.savedHost.isNotEmpty() || serverHostConfig.defaultHost.isNotEmpty())

            if (!hasHost) {
                navController.navigateReplace(ScreenStructure.Login)
                return@launch
            }

            val isLoggedIn = graphqlQuery.isLoggedIn()
            if (isLoggedIn) {
                navController.navigateReplace(RootHomeScreenStructure.Home)
            } else {
                navController.navigateReplace(ScreenStructure.Login)
            }
        }
    }
}
