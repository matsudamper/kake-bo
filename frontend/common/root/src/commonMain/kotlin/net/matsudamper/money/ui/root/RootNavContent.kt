package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import net.matsudamper.money.frontend.common.base.lib.rememberSaveableStateHolder
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeMonthlyCategoryScreen
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodAllContentUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodAllScreen
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodCategoryScreen
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodSubCategoryScreen
import net.matsudamper.money.frontend.common.ui.screen.root.home.monthly.RootHomeMonthlyScreen
import net.matsudamper.money.frontend.common.ui.screen.root.mail.HomeAddTabScreen
import net.matsudamper.money.frontend.common.ui.screen.root.mail.HomeAddTabScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.mail.ImportMailScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.mail.ImportedMailListScreen
import net.matsudamper.money.frontend.common.ui.screen.root.mail.ImportedMailListScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.mail.MailImportScreen
import net.matsudamper.money.frontend.common.ui.screen.root.settings.RootSettingScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageCalendarScreen
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageCalendarScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageHostScreen
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageHostScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageListScreen
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageListScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.LocalGlobalEventHandlerLoginCheckUseCaseDelegate
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeAnalyticsSubCategoryApi
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabPeriodCategoryContentViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabPeriodSubCategoryContentViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabScreenApi
import net.matsudamper.money.frontend.common.viewmodel.root.home.monthly.RootHomeMonthlyScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.monthly.category.RootHomeMonthlyCategoryScreenViewModel
import net.matsudamper.money.frontend.graphql.GraphqlClient

private enum class SavedStateHolderKey {
    TabHolder,
    UsageHost,
}

@Composable
internal fun RootNavContent(
    current: ScreenStructure.Root,
    navController: ScreenNavController,
    viewModelEventHandlers: ViewModelEventHandlers,
    globalEvent: GlobalEvent,
    rootCoroutineScope: CoroutineScope,
    globalEventSender: EventSender<GlobalEvent>,
    usageListUiStateProvider: @Composable () -> RootUsageListScreenUiState,
    usageCalendarUiStateProvider: @Composable (ScreenStructure.Root.Usage.Calendar.YearMonth?) -> RootUsageCalendarScreenUiState,
    importMailScreenUiStateProvider: @Composable (ScreenStructure.Root.Add.Imported) -> ImportedMailListScreenUiState,
    importMailLinkScreenUiStateProvider: @Composable (ScreenStructure.Root.Add.Import) -> ImportMailScreenUiState,
    rootHomeTabPeriodAllContentUiStateProvider: @Composable (RootHomeScreenStructure.Period) -> RootHomeTabPeriodAllContentUiState,
    homeAddTabScreenUiStateProvider: @Composable () -> HomeAddTabScreenUiState,
    settingUiStateProvider: @Composable () -> RootSettingScreenUiState,
    windowInsets: PaddingValues,
) {
    val tabHolder: SaveableStateHolder = rememberSaveableStateHolder(
        id = SavedStateHolderKey.TabHolder,
    )
    val koin = LocalKoin.current
    val usageHost = rememberSaveableStateHolder(SavedStateHolderKey.UsageHost)
    val loginCheckUseCase = LocalGlobalEventHandlerLoginCheckUseCaseDelegate.current
    when (current) {
        is RootHomeScreenStructure -> {
            tabHolder.SaveableStateProvider(RootHomeScreenStructure::class.toString()) {
                val holder = rememberSaveableStateHolder(RootHomeScreenStructure::class.toString())

                when (current) {
                    is RootHomeScreenStructure.Monthly -> {
                        holder.SaveableStateProvider(RootHomeScreenStructure.Monthly::class.simpleName!!) {
                            val viewModel = LocalScopedObjectStore.current.putOrGet<RootHomeMonthlyScreenViewModel>(Unit) {
                                RootHomeMonthlyScreenViewModel(
                                    scopedObjectFeature = it,
                                    loginCheckUseCase = loginCheckUseCase,
                                    argument = current,
                                    graphqlClient = koin.get<GraphqlClient>(),
                                    navController = navController,
                                )
                            }
                            LaunchedEffect(viewModel, current) {
                                viewModel.updateStructure(current)
                            }
                            LaunchedEffect(viewModel.eventHandler) {
                                viewModelEventHandlers.handleRootHomeMonthlyScreen(viewModel.eventHandler)
                            }
                            RootHomeMonthlyScreen(
                                modifier = Modifier,
                                uiState = viewModel.uiStateFlow.collectAsState().value,
                                windowInsets = windowInsets,
                            )
                        }
                    }

                    is RootHomeScreenStructure.Period -> {
                        when (current) {
                            is RootHomeScreenStructure.Home,
                            is RootHomeScreenStructure.PeriodAnalytics,
                            -> {
                                holder.SaveableStateProvider(RootHomeScreenStructure.Period::class.simpleName!!) {
                                    RootHomeTabPeriodAllScreen(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = windowInsets,
                                        uiState = rootHomeTabPeriodAllContentUiStateProvider(current),
                                    )
                                }
                            }

                            is RootHomeScreenStructure.PeriodCategory -> {
                                val categoryViewModel = LocalScopedObjectStore.current.putOrGet(Unit) {
                                    RootHomeTabPeriodCategoryContentViewModel(
                                        initialCategoryId = current.categoryId,
                                        scopedObjectFeature = it,
                                        api = RootHomeTabScreenApi(
                                            graphqlClient = koin.get(),
                                        ),
                                        loginCheckUseCase = loginCheckUseCase,
                                        navController = navController,
                                    )
                                }
                                LaunchedEffect(categoryViewModel.eventHandler) {
                                    viewModelEventHandlers.handleRootHomeTabPeriodCategoryContent(categoryViewModel.eventHandler)
                                }
                                LaunchedEffect(categoryViewModel, current) {
                                    categoryViewModel.updateStructure(current)
                                }
                                RootHomeTabPeriodCategoryScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    uiState = categoryViewModel.uiStateFlow.collectAsState().value,
                                    windowInsets = windowInsets,
                                )
                            }
                        }
                    }

                    is RootHomeScreenStructure.PeriodSubCategory -> {
                        val subCategoryViewModel = LocalScopedObjectStore.current.putOrGet(Unit) {
                            RootHomeTabPeriodSubCategoryContentViewModel(
                                structure = current,
                                scopedObjectFeature = it,
                                loginCheckUseCase = loginCheckUseCase,
                                navController = navController,
                                api = RootHomeAnalyticsSubCategoryApi(
                                    graphqlClient = koin.get(),
                                ),
                            )
                        }
                        LaunchedEffect(subCategoryViewModel.eventHandler) {
                            viewModelEventHandlers.handleRootHomeTabPeriodSubCategoryContent(subCategoryViewModel.eventHandler)
                        }
                        RootHomeTabPeriodSubCategoryScreen(
                            modifier = Modifier.fillMaxSize(),
                            uiState = subCategoryViewModel.uiStateFlow.collectAsState().value,
                            windowInsets = windowInsets,
                        )
                    }

                    is RootHomeScreenStructure.MonthlyCategory -> {
                        val monthlyCategoryViewModel = LocalScopedObjectStore.current.putOrGet<RootHomeMonthlyCategoryScreenViewModel>(Unit) {
                            RootHomeMonthlyCategoryScreenViewModel(
                                argument = current,
                                scopedObjectFeature = it,
                                loginCheckUseCase = loginCheckUseCase,
                                graphqlClient = koin.get<GraphqlClient>(),
                                navController = navController,
                            )
                        }
                        LaunchedEffect(monthlyCategoryViewModel.eventHandler) {
                            viewModelEventHandlers.handleRootHomeMonthlyCategoryScreen(monthlyCategoryViewModel.eventHandler)
                        }
                        LaunchedEffect(monthlyCategoryViewModel, current) {
                            monthlyCategoryViewModel.updateStructure(current)
                        }

                        RootHomeMonthlyCategoryScreen(
                            modifier = Modifier.fillMaxSize(),
                            uiState = monthlyCategoryViewModel.uiStateFlow.collectAsState().value,
                            windowInsets = windowInsets,
                        )
                    }

                    is RootHomeScreenStructure.MonthlySubCategory -> {
                        val monthlySubCategoryViewModel = LocalScopedObjectStore.current.putOrGet<net.matsudamper.money.frontend.common.viewmodel.root.home.monthly.subcategory.RootHomeMonthlySubCategoryScreenViewModel>(
                            Unit,
                        ) {
                            net.matsudamper.money.frontend.common.viewmodel.root.home.monthly.subcategory.RootHomeMonthlySubCategoryScreenViewModel(
                                argument = current,
                                scopedObjectFeature = it,
                                loginCheckUseCase = loginCheckUseCase,
                                graphqlClient = koin.get<GraphqlClient>(),
                                navController = navController,
                            )
                        }
                        LaunchedEffect(monthlySubCategoryViewModel.eventHandler) {
                            viewModelEventHandlers.handleRootHomeMonthlySubCategoryScreen(monthlySubCategoryViewModel.eventHandler)
                        }
                        LaunchedEffect(monthlySubCategoryViewModel, current) {
                            monthlySubCategoryViewModel.updateStructure(current)
                        }

                        net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeMonthlySubCategoryScreen(
                            modifier = Modifier.fillMaxSize(),
                            uiState = monthlySubCategoryViewModel.uiStateFlow.collectAsState().value,
                            windowInsets = windowInsets,
                        )
                    }
                }
            }
        }

        is ScreenStructure.Root.Usage -> {
            tabHolder.SaveableStateProvider(ScreenStructure.Root.Usage::class.toString()) {
                val rootScreen = remember {
                    movableContentOf { rootUiState: RootUsageHostScreenUiState, content: @Composable () -> Unit ->
                        RootUsageHostScreen(
                            modifier = Modifier.fillMaxSize(),
                            uiState = rootUiState,
                            windowInsets = windowInsets,
                        ) {
                            content()
                        }
                    }
                }

                when (current) {
                    is ScreenStructure.Root.Usage.Calendar -> {
                        usageHost.SaveableStateProvider(current::class.toString()) {
                            val uiState = usageCalendarUiStateProvider(current.yearMonth)
                            rootScreen(uiState.hostScreenUiState) {
                                RootUsageCalendarScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    uiState = uiState,
                                )
                            }
                        }
                    }

                    is ScreenStructure.Root.Usage.List -> {
                        usageHost.SaveableStateProvider(ScreenStructure.Root.Usage.List::class.toString()) {
                            val uiState = usageListUiStateProvider()
                            rootScreen(uiState.hostScreenUiState) {
                                RootUsageListScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    uiState = uiState,
                                )
                            }
                        }
                    }
                }
            }
        }

        is ScreenStructure.Root.Add -> {
            tabHolder.SaveableStateProvider(current::class.toString()) {
                when (current) {
                    is ScreenStructure.Root.Add.Root -> {
                        HomeAddTabScreen(
                            uiState = homeAddTabScreenUiStateProvider(),
                            windowInsets = windowInsets,
                        )
                    }

                    is ScreenStructure.Root.Add.Import -> {
                        MailImportScreen(
                            uiState = importMailLinkScreenUiStateProvider(current),
                            windowInsets = windowInsets,
                        )
                    }

                    is ScreenStructure.Root.Add.Imported -> {
                        ImportedMailListScreen(
                            uiState = importMailScreenUiStateProvider(current),
                            windowInsets = windowInsets,
                        )
                    }
                }
            }
        }

        is ScreenStructure.Root.Settings -> {
            tabHolder.SaveableStateProvider(current::class.toString()) {
                SettingNavContent(
                    state = current,
                    globalEventSender = globalEventSender,
                    navController = navController,
                    globalEvent = globalEvent,
                    viewModelEventHandlers = viewModelEventHandlers,
                    settingUiStateProvider = settingUiStateProvider,
                    windowInsets = windowInsets,
                )
            }
        }
    }
}
