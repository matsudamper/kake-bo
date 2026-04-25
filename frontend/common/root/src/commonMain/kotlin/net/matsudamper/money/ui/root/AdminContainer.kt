package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.admin.rememberAdminScreenController
import net.matsudamper.money.frontend.common.ui.screen.admin.AdminRootScreen
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminAddUserScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminLoginScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminRootScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminUnlinkedImagesScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminUserSearchScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.graphql.GraphqlAdminQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient

@Composable
internal fun AdminContainer(
    windowInsets: PaddingValues,
    globalEventSender: EventSender<GlobalEvent>,
) {
    val koin = LocalKoin.current
    val controller = rememberAdminScreenController()

    val adminRootViewModel = LocalScopedObjectStore.current.putOrGet<AdminRootScreenViewModel>(Unit) {
        AdminRootScreenViewModel(
            scopedObjectFeature = it,
            controller = controller,
            adminQuery = GraphqlAdminQuery(koin.get<GraphqlClient>()),
            globalEventSender = globalEventSender,
        )
    }
    AdminRootScreen(
        adminScreenController = controller,
        adminLoginScreenUiStateProvider = {
            val loginViewModel = LocalScopedObjectStore.current.putOrGet<AdminLoginScreenViewModel>(Unit) {
                AdminLoginScreenViewModel(
                    scopedObjectFeature = it,
                    controller = controller,
                    adminQuery = GraphqlAdminQuery(koin.get<GraphqlClient>()),
                    globalEventSender = globalEventSender,
                )
            }
            loginViewModel.uiStateFlow.collectAsState().value
        },
        adminRootScreenUiStateProvider = {
            adminRootViewModel.uiStateFlow.collectAsState().value
        },
        adminAddUserUiStateProvider = {
            val adminAddUserScreenViewModel = LocalScopedObjectStore.current.putOrGet<AdminAddUserScreenViewModel>(Unit) {
                AdminAddUserScreenViewModel(
                    scopedObjectFeature = it,
                    adminQuery = GraphqlAdminQuery(koin.get<GraphqlClient>()),
                )
            }
            adminAddUserScreenViewModel.uiStateFlow.collectAsState().value
        },
        adminUnlinkedImagesUiStateProvider = {
            val adminUnlinkedImagesScreenViewModel = LocalScopedObjectStore.current.putOrGet<AdminUnlinkedImagesScreenViewModel>(Unit) {
                AdminUnlinkedImagesScreenViewModel(
                    scopedObjectFeature = it,
                    graphqlClient = koin.get<GraphqlClient>(),
                    adminQuery = GraphqlAdminQuery(koin.get<GraphqlClient>()),
                )
            }
            adminUnlinkedImagesScreenViewModel.uiStateFlow.collectAsState().value
        },
        adminUserSearchUiStateProvider = {
            val adminUserSearchScreenViewModel = LocalScopedObjectStore.current.putOrGet<AdminUserSearchScreenViewModel>(Unit) {
                val graphqlClient = koin.get<GraphqlClient>()
                AdminUserSearchScreenViewModel(
                    scopedObjectFeature = it,
                    adminQuery = GraphqlAdminQuery(graphqlClient),
                    pagingModel = net.matsudamper.money.frontend.common.viewmodel.admin.AdminUserSearchPagingModel(graphqlClient),
                )
            }
            adminUserSearchScreenViewModel.uiStateFlow.collectAsState().value
        },
        windowInsets = windowInsets,
    )
}
