package screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import event.ViewModelEventHandlers
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodAllContent
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodCategoryContent
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodContent
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabScreen
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
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabPeriodScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabScreenApi
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabScreenViewModel

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
    rootUsageHostUiStateProvider: @Composable () -> RootUsageHostScreenUiState,
    usageListUiStateProvider: @Composable () -> RootUsageListScreenUiState,
    usageCalendarUiStateProvider: @Composable () -> RootUsageCalendarScreenUiState,
    importMailScreenUiStateProvider: @Composable (ScreenStructure.Root.Mail.Imported) -> ImportedMailListScreenUiState,
    importMailLinkScreenUiStateProvider: @Composable (ScreenStructure.Root.Mail.Import) -> ImportMailScreenUiState,
    settingUiStateProvider: @Composable () -> RootSettingScreenUiState,
) {
    val usageHost = rememberSaveableStateHolder()
    when (current) {
        is RootHomeScreenStructure -> {
            tabHolder.SaveableStateProvider(current::class.toString()) {
                val viewModel = remember {
                    RootHomeTabScreenViewModel(
                        coroutineScope = rootCoroutineScope,
                        loginCheckUseCase = loginCheckUseCase,
                    )
                }
                LaunchedEffect(viewModel.viewModelEventHandler) {
                    viewModelEventHandlers.handle(
                        handler = viewModel.viewModelEventHandler,
                    )
                }
                LaunchedEffect(viewModel, current) {
                    viewModel.updateScreenStructure(current)
                }
                RootHomeTabScreen(
                    uiState = viewModel.uiStateFlow.collectAsState().value,
                    scaffoldListener = rootScreenScaffoldListener,
                    content = {
                        val holder = rememberSaveableStateHolder()
                        when (current) {
                            is RootHomeScreenStructure.Monthly -> {
                                holder.SaveableStateProvider(RootHomeScreenStructure.Monthly::class) {
                                }
                            }

                            is RootHomeScreenStructure.Period -> {
                                holder.SaveableStateProvider(RootHomeScreenStructure.Period::class) {
                                    val periodViewModel = remember {
                                        RootHomeTabPeriodScreenViewModel(
                                            coroutineScope = rootCoroutineScope,
                                            api = RootHomeTabScreenApi(),
                                        )
                                    }
                                    LaunchedEffect(current, periodViewModel) {
                                        periodViewModel.updateScreenStructure(current)
                                    }
                                    RootHomeTabPeriodContent(
                                        modifier = Modifier.fillMaxSize(),
                                        uiState = periodViewModel.uiStateFlow.collectAsState().value,
                                    ) {
                                        when (current) {
                                            is RootHomeScreenStructure.Home,
                                            is RootHomeScreenStructure.PeriodAnalytics,
                                            -> {
                                                holder.SaveableStateProvider(RootHomeScreenStructure.PeriodAnalytics::class) {
                                                    LaunchedEffect(periodViewModel.viewModelEventHandler) {
                                                        viewModelEventHandlers.handle(periodViewModel.viewModelEventHandler)
                                                    }

                                                    val allContentViewModel = remember {
                                                        RootHomeTabPeriodAllContentViewModel(
                                                            coroutineScope = rootCoroutineScope,
                                                            api = RootHomeTabScreenApi(),
                                                        )
                                                    }
                                                    LaunchedEffect(allContentViewModel, current) {
                                                        allContentViewModel.updateStructure(current)
                                                    }
                                                    RootHomeTabPeriodAllContent(
                                                        modifier = Modifier.fillMaxSize(),
                                                        uiState = allContentViewModel.uiStateFlow.collectAsState().value,
                                                    )
                                                }
                                            }

                                            is RootHomeScreenStructure.PeriodSubCategory -> {
                                                val categoryViewModel = remember {
                                                    RootHomeTabPeriodCategoryContentViewModel(
                                                        categoryId = current.categoryId,
                                                        coroutineScope = rootCoroutineScope,
                                                        api = RootHomeTabScreenApi(),
                                                    )
                                                }
                                                RootHomeTabPeriodCategoryContent(
                                                    modifier = Modifier.fillMaxSize(),
                                                    uiState = categoryViewModel.uiStateFlow.collectAsState().value,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }

        is ScreenStructure.Root.Usage -> {
            tabHolder.SaveableStateProvider(ScreenStructure.Root.Usage::class.toString()) {
                val hostUiState = rootUsageHostUiStateProvider()
                RootUsageHostScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = hostUiState,
                    listener = rootScreenScaffoldListener,
                ) {
                    when (current) {
                        is ScreenStructure.Root.Usage.Calendar -> {
                            usageHost.SaveableStateProvider(current::class.toString()) {
                                val uiState = usageCalendarUiStateProvider()
                                RootUsageCalendarScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    uiState = uiState,
                                )
                            }
                        }

                        is ScreenStructure.Root.Usage.List -> {
                            usageHost.SaveableStateProvider(current::class.toString()) {
                                val uiState = usageListUiStateProvider()
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
