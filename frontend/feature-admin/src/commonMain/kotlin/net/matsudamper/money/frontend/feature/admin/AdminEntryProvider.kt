package net.matsudamper.money.frontend.feature.admin

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.addEntryProvider
import net.matsudamper.money.frontend.common.base.nav.user.IScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.feature.admin.ui.AddUserScreen
import net.matsudamper.money.frontend.feature.admin.ui.AdminLoginScreen
import net.matsudamper.money.frontend.feature.admin.ui.AdminRootScreen
import net.matsudamper.money.frontend.feature.admin.ui.AdminUnlinkedImagesScreen
import net.matsudamper.money.frontend.feature.admin.ui.user.UserSearchScreen
import net.matsudamper.money.frontend.feature.admin.viewmodel.AdminAddUserScreenViewModel
import net.matsudamper.money.frontend.feature.admin.viewmodel.AdminLoginScreenViewModel
import net.matsudamper.money.frontend.feature.admin.viewmodel.AdminRootScreenViewModel
import net.matsudamper.money.frontend.feature.admin.viewmodel.AdminUnlinkedImagesScreenViewModel
import net.matsudamper.money.frontend.feature.admin.viewmodel.AdminUserSearchPagingModel
import net.matsudamper.money.frontend.feature.admin.viewmodel.AdminUserSearchScreenViewModel
import net.matsudamper.money.frontend.graphql.GraphqlAdminQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.common.di.LocalKoin

@OptIn(ExperimentalMaterial3Api::class)
public object AdminEntryProvider {
    public fun EntryProviderScope<IScreenStructure>.addProviders(
        navController: ScreenNavController,
        globalEventSender: EventSender<GlobalEvent>,
        paddingValues: State<PaddingValues>,
    ) {
        addEntryProvider<ScreenStructure.Admin.Root, IScreenStructure> { current ->
            val koin = LocalKoin.current
            val adminRootViewModel = LocalScopedObjectStore.current.putOrGet<AdminRootScreenViewModel>(Unit) {
                AdminRootScreenViewModel(
                    scopedObjectFeature = it,
                    navController = navController,
                    adminQuery = GraphqlAdminQuery(koin.get<GraphqlClient>()),
                    globalEventSender = globalEventSender,
                )
            }
            AdminRootScreen(
                uiState = adminRootViewModel.uiStateFlow.collectAsState().value,
                modifier = Modifier.padding(paddingValues.value),
            )
        }
        addEntryProvider<ScreenStructure.Admin.Login, IScreenStructure> { current ->
            val koin = LocalKoin.current
            val loginViewModel = LocalScopedObjectStore.current.putOrGet<AdminLoginScreenViewModel>(Unit) {
                AdminLoginScreenViewModel(
                    scopedObjectFeature = it,
                    navController = navController,
                    adminQuery = GraphqlAdminQuery(koin.get<GraphqlClient>()),
                    globalEventSender = globalEventSender,
                )
            }
            AdminLoginScreen(
                uiState = loginViewModel.uiStateFlow.collectAsState().value,
            )
        }
        addEntryProvider<ScreenStructure.Admin.AddUser, IScreenStructure> { current ->
            val koin = LocalKoin.current
            val adminAddUserScreenViewModel = LocalScopedObjectStore.current.putOrGet<AdminAddUserScreenViewModel>(Unit) {
                AdminAddUserScreenViewModel(
                    scopedObjectFeature = it,
                    adminQuery = GraphqlAdminQuery(koin.get<GraphqlClient>()),
                )
            }
            BasicAlertDialog(
                onDismissRequest = { navController.back() },
            ) {
                AddUserScreen(
                    uiState = adminAddUserScreenViewModel.uiStateFlow.collectAsState().value,
                )
            }
        }
        addEntryProvider<ScreenStructure.Admin.UnlinkedImages, IScreenStructure> { current ->
            val koin = LocalKoin.current
            val adminUnlinkedImagesScreenViewModel = LocalScopedObjectStore.current.putOrGet<AdminUnlinkedImagesScreenViewModel>(Unit) {
                AdminUnlinkedImagesScreenViewModel(
                    scopedObjectFeature = it,
                    graphqlClient = koin.get<GraphqlClient>(),
                    adminQuery = GraphqlAdminQuery(koin.get<GraphqlClient>()),
                )
            }
            AdminUnlinkedImagesScreen(
                uiState = adminUnlinkedImagesScreenViewModel.uiStateFlow.collectAsState().value,
                onClickBack = { navController.back() },
                modifier = Modifier.padding(paddingValues.value),
            )
        }
        addEntryProvider<ScreenStructure.Admin.UserSearch, IScreenStructure> { current ->
            val koin = LocalKoin.current
            val graphqlClient = koin.get<GraphqlClient>()
            val adminUserSearchScreenViewModel = LocalScopedObjectStore.current.putOrGet<AdminUserSearchScreenViewModel>(Unit) {
                AdminUserSearchScreenViewModel(
                    scopedObjectFeature = it,
                    adminQuery = GraphqlAdminQuery(graphqlClient),
                    pagingModel = AdminUserSearchPagingModel(graphqlClient),
                )
            }
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                adminUserSearchScreenViewModel.eventHandler.collect(
                    object : AdminUserSearchScreenViewModel.Event {
                        override fun showSnackBar(message: String) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    },
                )
            }
            UserSearchScreen(
                uiState = adminUserSearchScreenViewModel.uiStateFlow.collectAsState().value,
                onClickBack = { navController.back() },
                snackbarHostState = snackbarHostState,
                modifier = Modifier.padding(paddingValues.value),
            )
        }
    }
}
