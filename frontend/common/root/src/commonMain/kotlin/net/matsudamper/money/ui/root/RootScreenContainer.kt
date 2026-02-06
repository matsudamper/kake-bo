package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootHostScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.SettingViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.mail.HomeAddTabScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.RootUsageHostViewModel
import net.matsudamper.money.ui.root.viewmodel.LocalViewModelProviders

@Composable
internal fun RootScreenContainer(
    current: ScreenStructure.Root,
    settingViewModel: SettingViewModel,
    navController: ScreenNavController,
    mailScreenViewModel: HomeAddTabScreenViewModel,
    rootUsageHostViewModel: RootUsageHostViewModel,
    viewModelEventHandlers: ViewModelEventHandlers,
    rootCoroutineScope: CoroutineScope,
    globalEventSender: EventSender<GlobalEvent>,
    globalEvent: GlobalEvent,
    windowInsets: PaddingValues,
) {
    val windowInsets = WindowInsets.safeDrawing.asPaddingValues()
    var currentScreen by remember { mutableStateOf(RootScreenTab.Home) }
    val listener = remember(navController) {
        object : RootScreenScaffoldListener {
            override val kakeboScaffoldListener: KakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navController.navigateToHome()
                }
            }

            override fun onClickHome() {
                navController.navigate(RootHomeScreenStructure.Home)
            }

            override fun onClickList() {
                navController.navigate(ScreenStructure.Root.Usage.Calendar())
            }

            override fun onClickSettings() {
                navController.navigate(ScreenStructure.Root.Settings.Root)
            }

            override fun onClickAdd() {
                navController.navigate(ScreenStructure.Root.Add.Root)
            }
        }
    }
    LaunchedEffect(current, settingViewModel) {
        when (current) {
            is RootHomeScreenStructure -> {
                currentScreen = RootScreenTab.Home
            }

            is ScreenStructure.Root.Add -> {
                currentScreen = RootScreenTab.Add
                mailScreenViewModel.updateScreenStructure(current)
            }

            is ScreenStructure.Root.Settings -> {
                currentScreen = RootScreenTab.Settings
                settingViewModel.updateLastStructure(current)
            }

            is ScreenStructure.Root.Usage -> {
                currentScreen = RootScreenTab.List
                rootUsageHostViewModel.updateStructure(current)
            }
        }
    }
    LaunchedEffect(viewModelEventHandlers, settingViewModel.backgroundEventHandler) {
        viewModelEventHandlers.handleSetting(settingViewModel.backgroundEventHandler)
    }
    RootHostScaffold(
        modifier = Modifier.fillMaxSize(),
        currentScreen = currentScreen,
        listener = listener,
        windowInsets = windowInsets,
    ) {
        RootNavContent(
            windowInsets = windowInsets,
            navController = navController,
            current = current,
            viewModelEventHandlers = viewModelEventHandlers,
            rootCoroutineScope = rootCoroutineScope,
            globalEventSender = globalEventSender,
            globalEvent = globalEvent,
            homeAddTabScreenUiStateProvider = {
                mailScreenViewModel.uiStateFlow.collectAsState().value
            },
            usageCalendarUiStateProvider = { yearMonth ->
                val coroutineScope = rememberCoroutineScope()
                val viewModel = LocalViewModelProviders.current
                    .moneyUsagesCalendarViewModel(
                        coroutineScope = coroutineScope,
                        rootUsageHostViewModel = rootUsageHostViewModel,
                        yearMonth = yearMonth,
                    )

                LaunchedEffect(viewModel.viewModelEventHandler) {
                    viewModelEventHandlers.handleMoneyUsagesCalendar(
                        handler = viewModel.viewModelEventHandler,
                    )
                }
                viewModel.uiStateFlow.collectAsState().value
            },
            usageListUiStateProvider = {
                val coroutineScope = rememberCoroutineScope()
                val viewModel = LocalViewModelProviders.current
                    .moneyUsagesListViewModel(
                        coroutineScope = coroutineScope,
                        rootUsageHostViewModel = rootUsageHostViewModel,
                    )
                LaunchedEffect(viewModel.viewModelEventHandler) {
                    viewModelEventHandlers.handleMoneyUsagesList(
                        handler = viewModel.viewModelEventHandler,
                    )
                }
                viewModel.uiStateFlow.collectAsState().value
            },
            importMailLinkScreenUiStateProvider = {
                val mailImportViewModel = LocalViewModelProviders.current
                    .mailImportViewModel()
                LaunchedEffect(mailImportViewModel.eventHandler) {
                    viewModelEventHandlers.handleMailImport(mailImportViewModel.eventHandler)
                }

                mailImportViewModel.rootUiStateFlow.collectAsState().value
            },
            importMailScreenUiStateProvider = { screenStructure ->
                val importedMailListViewModel = LocalViewModelProviders.current
                    .importedMailListViewModel()
                LaunchedEffect(screenStructure) {
                    importedMailListViewModel.updateQuery(screenStructure)
                }
                LaunchedEffect(importedMailListViewModel.eventHandler) {
                    viewModelEventHandlers.handleMailLink(importedMailListViewModel.eventHandler)
                }
                importedMailListViewModel.rootUiStateFlow.collectAsState().value
            },
            settingUiStateProvider = {
                settingViewModel.uiState.collectAsState().value
            },
            rootHomeTabPeriodAllContentUiStateProvider = { current ->
                val allContentViewModel = LocalViewModelProviders.current
                    .rootHomeTabPeriodAllContentViewModel()
                LaunchedEffect(allContentViewModel, current) {
                    allContentViewModel.updateStructure(current)
                }
                LaunchedEffect(allContentViewModel.eventHandler) {
                    viewModelEventHandlers.handleRootHomeTabPeriodAllContent(allContentViewModel.eventHandler)
                }
                allContentViewModel.uiStateFlow.collectAsState().value
            },
        )
    }
}
