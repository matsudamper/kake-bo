package screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import event.ViewModelEventHandlers
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeMonthlyCategoryScreen
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodAllScreen
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodCategoryScreen
import net.matsudamper.money.frontend.common.ui.screen.root.home.monthly.RootHomeMonthlyScreen
import net.matsudamper.money.frontend.common.ui.screen.root.mail.HomeMailTabScreen
import net.matsudamper.money.frontend.common.ui.screen.root.mail.HomeMailTabScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.mail.ImportMailScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.mail.ImportedMailListScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.settings.RootSettingScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageCalendarScreen
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageCalendarScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageHostScreen
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageHostScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageListScreen
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageListScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabPeriodAllContentViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabPeriodCategoryContentViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabScreenApi
import net.matsudamper.money.frontend.common.viewmodel.root.home.monthly.RootHomeMonthlyScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.monthly.category.RootHomeMonthlyCategoryScreenViewModel

@Composable
internal fun RootNavContent(
    tabHolder: SaveableStateHolder,
    current: ScreenStructure.Root,
    rootScreenScaffoldListener: RootScreenScaffoldListener,
    viewModelEventHandlers: ViewModelEventHandlers,
    globalEvent: GlobalEvent,
    rootCoroutineScope: CoroutineScope,
    globalEventSender: EventSender<GlobalEvent>,
    loginCheckUseCase: LoginCheckUseCase,
    homeMailTabScreenUiStateProvider: @Composable () -> HomeMailTabScreenUiState,
    usageListUiStateProvider: @Composable () -> RootUsageListScreenUiState,
    usageCalendarUiStateProvider: @Composable (ScreenStructure.Root.Usage.Calendar.YearMonth?) -> RootUsageCalendarScreenUiState,
    importMailScreenUiStateProvider: @Composable (ScreenStructure.Root.Mail.Imported) -> ImportedMailListScreenUiState,
    importMailLinkScreenUiStateProvider: @Composable (ScreenStructure.Root.Mail.Import) -> ImportMailScreenUiState,
    settingUiStateProvider: @Composable () -> RootSettingScreenUiState,
) {
    val usageHost = rememberSaveableStateHolder()
    when (current) {
        is RootHomeScreenStructure -> {
            tabHolder.SaveableStateProvider(current::class.toString()) {
                val holder = rememberSaveableStateHolder()

                when (current) {
                    is RootHomeScreenStructure.Monthly -> {
                        holder.SaveableStateProvider(RootHomeScreenStructure.Monthly::class) {
                            val coroutineScope = rememberCoroutineScope()
                            val viewModel =
                                remember {
                                    RootHomeMonthlyScreenViewModel(
                                        coroutineScope = coroutineScope,
                                        loginCheckUseCase = loginCheckUseCase,
                                        argument = current,
                                    )
                                }
                            LaunchedEffect(viewModel, current) {
                                viewModel.updateStructure(current)
                            }
                            LaunchedEffect(viewModel.eventHandler) {
                                viewModelEventHandlers.handle(viewModel.eventHandler)
                            }
                            RootHomeMonthlyScreen(
                                modifier = Modifier,
                                scaffoldListener = rootScreenScaffoldListener,
                                uiState = viewModel.uiStateFlow.collectAsState().value,
                            )
                        }
                    }

                    is RootHomeScreenStructure.Period -> {
                        when (current) {
                            is RootHomeScreenStructure.Home,
                            is RootHomeScreenStructure.PeriodAnalytics,
                            -> {
                                holder.SaveableStateProvider(RootHomeScreenStructure.Period::class) {
                                    val allContentViewModel =
                                        remember {
                                            RootHomeTabPeriodAllContentViewModel(
                                                coroutineScope = rootCoroutineScope,
                                                api = RootHomeTabScreenApi(),
                                                loginCheckUseCase = loginCheckUseCase,
                                            )
                                        }
                                    LaunchedEffect(allContentViewModel, current) {
                                        allContentViewModel.updateStructure(current)
                                    }
                                    LaunchedEffect(allContentViewModel.eventHandler) {
                                        viewModelEventHandlers.handle(allContentViewModel.eventHandler)
                                    }
                                    RootHomeTabPeriodAllScreen(
                                        modifier = Modifier.fillMaxSize(),
                                        uiState = allContentViewModel.uiStateFlow.collectAsState().value,
                                        scaffoldListener = rootScreenScaffoldListener,
                                    )
                                }
                            }

                            is RootHomeScreenStructure.PeriodCategory -> {
                                val categoryViewModel =
                                    remember {
                                        RootHomeTabPeriodCategoryContentViewModel(
                                            initialCategoryId = current.categoryId,
                                            coroutineScope = rootCoroutineScope,
                                            api = RootHomeTabScreenApi(),
                                            loginCheckUseCase = loginCheckUseCase,
                                        )
                                    }
                                LaunchedEffect(categoryViewModel.eventHandler) {
                                    viewModelEventHandlers.handle(categoryViewModel.eventHandler)
                                }
                                LaunchedEffect(categoryViewModel, current) {
                                    categoryViewModel.updateStructure(current)
                                }
                                RootHomeTabPeriodCategoryScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    uiState = categoryViewModel.uiStateFlow.collectAsState().value,
                                    scaffoldListener = rootScreenScaffoldListener,
                                )
                            }
                        }
                    }

                    is RootHomeScreenStructure.MonthlyCategory -> {
                        val monthlyCategoryViewModel =
                            remember {
                                RootHomeMonthlyCategoryScreenViewModel(
                                    argument = current,
                                    coroutineScope = rootCoroutineScope,
                                    loginCheckUseCase = loginCheckUseCase,
                                )
                            }
                        LaunchedEffect(monthlyCategoryViewModel.eventHandler) {
                            viewModelEventHandlers.handle(monthlyCategoryViewModel.eventHandler)
                        }
                        LaunchedEffect(monthlyCategoryViewModel, current) {
                            monthlyCategoryViewModel.updateStructure(current)
                        }

                        RootHomeMonthlyCategoryScreen(
                            modifier = Modifier.fillMaxSize(),
                            uiState = monthlyCategoryViewModel.uiStateFlow.collectAsState().value,
                            scaffoldListener = rootScreenScaffoldListener,
                        )
                    }
                }
            }
        }

        is ScreenStructure.Root.Usage -> {
            tabHolder.SaveableStateProvider(ScreenStructure.Root.Usage::class.toString()) {
                val rootScreen =
                    remember {
                        movableContentOf { rootUiState: RootUsageHostScreenUiState, content: @Composable () -> Unit ->
                            RootUsageHostScreen(
                                modifier = Modifier.fillMaxSize(),
                                uiState = rootUiState,
                                listener = rootScreenScaffoldListener,
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
                        usageHost.SaveableStateProvider(current::class.toString()) {
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

        is ScreenStructure.Root.Mail -> {
            tabHolder.SaveableStateProvider(current::class.toString()) {
                val uiState = homeMailTabScreenUiStateProvider()
                HomeMailTabScreen(
                    screenStructure = current,
                    uiState = uiState,
                    importMailScreenUiStateProvider = {
                        importMailLinkScreenUiStateProvider(it)
                    },
                    importedImportMailScreenUiStateProvider = {
                        importMailScreenUiStateProvider(it)
                    },
                    rootScreenScaffoldListener = rootScreenScaffoldListener,
                )
            }
        }

        is ScreenStructure.Root.Settings -> {
            tabHolder.SaveableStateProvider(current::class.toString()) {
                SettingNavContent(
                    state = current,
                    globalEventSender = globalEventSender,
                    globalEvent = globalEvent,
                    rootScreenScaffoldListener = rootScreenScaffoldListener,
                    viewModelEventHandlers = viewModelEventHandlers,
                    settingUiStateProvider = settingUiStateProvider,
                )
            }
        }
    }
}
