package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.settings.ApiSettingScreen
import net.matsudamper.money.frontend.common.ui.screen.root.settings.ImapConfigScreen
import net.matsudamper.money.frontend.common.ui.screen.root.settings.ImportedMailFilterCategoryScreen
import net.matsudamper.money.frontend.common.ui.screen.root.settings.LoginSettingScreen
import net.matsudamper.money.frontend.common.ui.screen.root.settings.RootSettingScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.settings.SettingCategoriesScreen
import net.matsudamper.money.frontend.common.ui.screen.root.settings.SettingCategoryScreen
import net.matsudamper.money.frontend.common.ui.screen.root.settings.SettingMailCategoryFiltersScreen
import net.matsudamper.money.frontend.common.ui.screen.root.settings.SettingRootScreen
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.ImapSettingViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.settings.api.ApiSettingScreenApi
import net.matsudamper.money.frontend.common.viewmodel.root.settings.api.ApiSettingScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilter.ImportedMailFilterCategoryScreenGraphqlApi
import net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilter.ImportedMailFilterCategoryViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilters.ImportedMailCategoryFilterScreenPagingModel
import net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilters.SettingImportedMailCategoryFilterApi
import net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilters.SettingMailCategoryFiltersViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.settings.login.LoginSettingScreenApi
import net.matsudamper.money.frontend.common.viewmodel.root.settings.login.LoginSettingViewModel
import net.matsudamper.money.frontend.common.viewmodel.settings.SettingCategoriesViewModel
import net.matsudamper.money.frontend.common.viewmodel.settings.SettingCategoryViewModel
import net.matsudamper.money.frontend.common.viewmodel.settings.SettingScreenCategoryApi
import net.matsudamper.money.frontend.common.viewmodel.shared.FidoApi
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.GraphqlUserConfigQuery
import net.matsudamper.money.ui.root.viewmodel.provideViewModel

@Composable
internal fun SettingNavContent(
    state: ScreenStructure.Root.Settings,
    globalEventSender: EventSender<GlobalEvent>,
    navController: ScreenNavController,
    globalEvent: GlobalEvent,
    settingUiStateProvider: @Composable () -> RootSettingScreenUiState,
    viewModelEventHandlers: ViewModelEventHandlers,
    windowInsets: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val koin = LocalKoin.current
    val holder = rememberSaveableStateHolder()
    when (state) {
        ScreenStructure.Root.Settings.Root -> {
            holder.SaveableStateProvider(state::class.toString()) {
                SettingRootScreen(
                    modifier = modifier.fillMaxSize(),
                    uiState = settingUiStateProvider(),
                    windowInsets = windowInsets,
                )
            }
        }

        ScreenStructure.Root.Settings.Categories -> {
            holder.SaveableStateProvider(state::class.toString()) {
                val viewModel = provideViewModel {
                    SettingCategoriesViewModel(
                        scopedObjectFeature = it,
                        api = SettingScreenCategoryApi(
                            apolloClient = koin.get<GraphqlClient>().apolloClient,
                        ),
                        navController = navController,
                    )
                }

                LaunchedEffect(viewModel.viewModelEventHandler) {
                    viewModelEventHandlers.handleSettingCategories(viewModel.viewModelEventHandler)
                }
                LaunchedEffect(globalEvent, viewModel.globalEventHandler) {
                    viewModel.globalEventHandler.collect(globalEvent)
                }

                SettingCategoriesScreen(
                    uiState = viewModel.uiState.collectAsState().value,
                    modifier = modifier,
                    windowInsets = windowInsets,
                )
            }
        }

        is ScreenStructure.Root.Settings.Category -> {
            holder.SaveableStateProvider(state::class.toString()) {
                val viewModel = LocalScopedObjectStore.current.putOrGet(state.id) {
                    SettingCategoryViewModel(
                        scopedObjectFeature = it,
                        api = SettingScreenCategoryApi(
                            apolloClient = koin.get<GraphqlClient>().apolloClient,
                        ),
                        categoryId = state.id,
                        navController = navController,
                    )
                }
                LaunchedEffect(viewModel.viewModelEventHandler) {
                    viewModelEventHandlers.handleSettingCategory(viewModel.viewModelEventHandler)
                }
                LaunchedEffect(viewModel.globalEventHandler) {
                    viewModel.globalEventHandler.collect(globalEvent)
                }
                SettingCategoryScreen(
                    uiState = viewModel.uiState.collectAsState().value,
                    modifier = modifier,
                    windowInsets = windowInsets,
                )
            }
        }

        ScreenStructure.Root.Settings.Imap -> {
            holder.SaveableStateProvider(state::class.toString()) {
                val viewModel = provideViewModel {
                    ImapSettingViewModel(
                        scopedObjectFeature = it,
                        graphqlQuery = GraphqlUserConfigQuery(
                            graphqlClient = koin.get(),
                        ),
                        globalEventSender = globalEventSender,
                        ioDispatchers = Dispatchers.IO,
                        navController = navController,
                    )
                }

                ImapConfigScreen(
                    uiState = viewModel.uiState.collectAsState().value,
                    modifier = modifier,
                    windowInsets = windowInsets,
                )
            }
        }

        ScreenStructure.Root.Settings.MailCategoryFilters -> {
            holder.SaveableStateProvider(state::class.toString()) {
                val viewModel = provideViewModel {
                    SettingMailCategoryFiltersViewModel(
                        scopedObjectFeature = it,
                        pagingModel = ImportedMailCategoryFilterScreenPagingModel(
                            scopedObjectFeature = it,
                            graphqlClient = koin.get(),
                        ),
                        api = SettingImportedMailCategoryFilterApi(
                            apolloClient = koin.get<GraphqlClient>().apolloClient,
                        ),
                        navController = navController,
                    )
                }
                LaunchedEffect(viewModel.eventHandler, viewModelEventHandlers) {
                    viewModelEventHandlers.handleSettingMailCategoryFilters(viewModel.eventHandler)
                }
                SettingMailCategoryFiltersScreen(
                    modifier = modifier.fillMaxSize(),
                    uiState = viewModel.uiStateFlow.collectAsState().value,
                    windowInsets = windowInsets,
                )
            }
        }

        is ScreenStructure.Root.Settings.MailCategoryFilter -> {
            holder.SaveableStateProvider(state::class.toString()) {
                val viewModel = provideViewModel {
                    ImportedMailFilterCategoryViewModel(
                        scopedObjectFeature = it,
                        id = state.id,
                        api = ImportedMailFilterCategoryScreenGraphqlApi(
                            apolloClient = koin.get<GraphqlClient>().apolloClient,
                        ),
                        graphqlClient = koin.get(),
                        navController = navController,
                    )
                }

                LaunchedEffect(viewModelEventHandlers, viewModel.eventHandler) {
                    viewModelEventHandlers.handleImportedMailFilterCategory(viewModel.eventHandler)
                }

                ImportedMailFilterCategoryScreen(
                    modifier = modifier.fillMaxSize(),
                    uiState = viewModel.uiStateFlow.collectAsState().value,
                    windowInsets = windowInsets,
                )
            }
        }

        ScreenStructure.Root.Settings.Login -> {
            holder.SaveableStateProvider(state::class.toString()) {
                val viewModel = provideViewModel {
                    LoginSettingViewModel(
                        scopedObjectFeature = it,
                        api = LoginSettingScreenApi(
                            apolloClient = koin.get<GraphqlClient>().apolloClient,
                        ),
                        fidoApi = FidoApi(
                            apolloClient = koin.get<GraphqlClient>().apolloClient,
                        ),
                        webAuthModel = koin.get(),
                        navController = navController,
                    )
                }

                LaunchedEffect(viewModelEventHandlers, viewModel.eventHandler) {
                    viewModelEventHandlers.handleLoginSetting(viewModel.eventHandler)
                }

                LoginSettingScreen(
                    modifier = modifier.fillMaxSize(),
                    uiState = viewModel.uiStateFlow.collectAsState().value,
                    windowInsets = windowInsets,
                )
            }
        }

        ScreenStructure.Root.Settings.Api -> {
            holder.SaveableStateProvider(state::class.toString()) {
                val viewModel = provideViewModel {
                    ApiSettingScreenViewModel(
                        scopedObjectFeature = it,
                        api = ApiSettingScreenApi(
                            apolloClient = koin.get<GraphqlClient>().apolloClient,
                        ),
                        navController = navController,
                    )
                }
                val snackbarHostState = remember { SnackbarHostState() }
                LaunchedEffect(viewModelEventHandlers, viewModel.eventHandler, snackbarHostState) {
                    viewModelEventHandlers.handleApiSettingScreen(
                        eventHandler = viewModel.eventHandler,
                        snackbarHostState = snackbarHostState,
                    )
                }
                ApiSettingScreen(
                    uiState = viewModel.uiStateFlow.collectAsState().value,
                    snackbarHostState = snackbarHostState,
                    modifier = modifier,
                    windowInsets = windowInsets,
                )
            }
        }
    }
}
