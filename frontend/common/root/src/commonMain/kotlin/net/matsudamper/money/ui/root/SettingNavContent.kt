package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
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
import net.matsudamper.money.frontend.graphql.GraphqlUserConfigQuery

@Composable
internal fun SettingNavContent(
    state: ScreenStructure.Root.Settings,
    globalEventSender: EventSender<GlobalEvent>,
    globalEvent: GlobalEvent,
    settingUiStateProvider: @Composable () -> RootSettingScreenUiState,
    viewModelEventHandlers: ViewModelEventHandlers,
    rootScreenScaffoldListener: RootScreenScaffoldListener,
) {
    val coroutineScope = rememberCoroutineScope()
    val holder = rememberSaveableStateHolder()
    when (state) {
        ScreenStructure.Root.Settings.Root -> {
            holder.SaveableStateProvider(state::class.toString()) {
                SettingRootScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = settingUiStateProvider(),
                    listener = rootScreenScaffoldListener,
                )
            }
        }

        ScreenStructure.Root.Settings.Categories -> {
            holder.SaveableStateProvider(state::class.toString()) {
                val viewModel =
                    remember {
                        SettingCategoriesViewModel(
                            coroutineScope = coroutineScope,
                            api = SettingScreenCategoryApi(),
                        )
                    }

                LaunchedEffect(viewModel.viewModelEventHandler) {
                    viewModelEventHandlers.handleSettingCategories(viewModel.viewModelEventHandler)
                }
                LaunchedEffect(globalEvent, viewModel.globalEventHandler) {
                    viewModel.globalEventHandler.collect(globalEvent)
                }

                SettingCategoriesScreen(
                    rootScreenScaffoldListener = rootScreenScaffoldListener,
                    uiState = viewModel.uiState.collectAsState().value,
                )
            }
        }

        is ScreenStructure.Root.Settings.Category -> {
            holder.SaveableStateProvider(state::class.toString()) {
                val viewModel =
                    remember {
                        SettingCategoryViewModel(
                            coroutineScope = coroutineScope,
                            api = SettingScreenCategoryApi(),
                            categoryId = state.id,
                        )
                    }
                LaunchedEffect(viewModel.viewModelEventHandler) {
                    viewModelEventHandlers.handleSettingCategory(viewModel.viewModelEventHandler)
                }
                LaunchedEffect(viewModel.globalEventHandler) {
                    viewModel.globalEventHandler.collect(globalEvent)
                }
                SettingCategoryScreen(
                    rootScreenScaffoldListener = rootScreenScaffoldListener,
                    uiState = viewModel.uiState.collectAsState().value,
                )
            }
        }

        ScreenStructure.Root.Settings.Imap -> {
            holder.SaveableStateProvider(state::class.toString()) {
                val viewModel =
                    remember {
                        ImapSettingViewModel(
                            coroutineScope = coroutineScope,
                            graphqlQuery = GraphqlUserConfigQuery(),
                            globalEventSender = globalEventSender,
                            ioDispatchers = Dispatchers.Unconfined,
                        )
                    }

                ImapConfigScreen(
                    uiState = viewModel.uiState.collectAsState().value,
                    listener = rootScreenScaffoldListener,
                )
            }
        }

        ScreenStructure.Root.Settings.MailCategoryFilters -> {
            holder.SaveableStateProvider(state::class.toString()) {
                val viewModel =
                    remember(coroutineScope) {
                        SettingMailCategoryFiltersViewModel(
                            coroutineScope = coroutineScope,
                            pagingModel =
                            ImportedMailCategoryFilterScreenPagingModel(
                                coroutineScope = coroutineScope,
                            ),
                            api = SettingImportedMailCategoryFilterApi(),
                        )
                    }
                LaunchedEffect(viewModel.eventHandler, viewModelEventHandlers) {
                    viewModelEventHandlers.handleSettingMailCategoryFilters(viewModel.eventHandler)
                }
                SettingMailCategoryFiltersScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = viewModel.uiStateFlow.collectAsState().value,
                    rootScreenScaffoldListener = rootScreenScaffoldListener,
                )
            }
        }

        is ScreenStructure.Root.Settings.MailCategoryFilter -> {
            holder.SaveableStateProvider(state::class.toString()) {
                val viewModel =
                    remember(coroutineScope) {
                        ImportedMailFilterCategoryViewModel(
                            coroutineScope = coroutineScope,
                            id = state.id,
                            api = ImportedMailFilterCategoryScreenGraphqlApi(),
                        )
                    }

                LaunchedEffect(viewModelEventHandlers, viewModel.eventHandler) {
                    viewModelEventHandlers.handleImportedMailFilterCategory(viewModel.eventHandler)
                }

                ImportedMailFilterCategoryScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = viewModel.uiStateFlow.collectAsState().value,
                    rootScreenScaffoldListener = rootScreenScaffoldListener,
                )
            }
        }

        ScreenStructure.Root.Settings.Login -> {
            holder.SaveableStateProvider(state::class.toString()) {
                val viewModel =
                    remember(coroutineScope) {
                        LoginSettingViewModel(
                            coroutineScope = coroutineScope,
                            api = LoginSettingScreenApi(),
                            fidoApi = FidoApi(),
                        )
                    }

                LaunchedEffect(viewModelEventHandlers, viewModel.eventHandler) {
                    viewModelEventHandlers.handleLoginSetting(viewModel.eventHandler)
                }

                LoginSettingScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = viewModel.uiStateFlow.collectAsState().value,
                    rootScreenScaffoldListener = rootScreenScaffoldListener,
                )
            }
        }

        ScreenStructure.Root.Settings.Api -> {
            holder.SaveableStateProvider(state::class.toString()) {
                val viewModel = remember {
                    ApiSettingScreenViewModel(
                        coroutineScope = coroutineScope,
                        api = ApiSettingScreenApi(),
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
                    rootScreenScaffoldListener = rootScreenScaffoldListener,
                    uiState = viewModel.uiStateFlow.collectAsState().value,
                    snackbarHostState = snackbarHostState,
                )
            }
        }
    }
}
