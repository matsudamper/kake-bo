package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import kotlinx.coroutines.CoroutineScope
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.SettingViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.mail.HomeAddTabScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.RootUsageHostViewModel
import net.matsudamper.money.ui.root.viewmodel.ViewModelProviders

@Composable
internal fun RootScreenContainer(
    current: ScreenStructure.Root,
    settingViewModel: SettingViewModel,
    mailScreenViewModel: HomeAddTabScreenViewModel,
    rootUsageHostViewModel: RootUsageHostViewModel,
    viewModelEventHandlers: ViewModelEventHandlers,
    holder: SaveableStateHolder,
    rootCoroutineScope: CoroutineScope,
    globalEventSender: EventSender<GlobalEvent>,
    globalEvent: GlobalEvent,
    rootScreenScaffoldListener: RootScreenScaffoldListener,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    LaunchedEffect(current, settingViewModel) {
        when (current) {
            is RootHomeScreenStructure -> {
            }

            is ScreenStructure.Root.Add -> {
                mailScreenViewModel.updateScreenStructure(current)
            }

            is ScreenStructure.Root.Settings -> {
                settingViewModel.updateLastStructure(current)
            }

            is ScreenStructure.Root.Usage -> {
                rootUsageHostViewModel.updateStructure(current)
            }
        }
    }
    LaunchedEffect(viewModelEventHandlers, settingViewModel.backgroundEventHandler) {
        viewModelEventHandlers.handleSetting(settingViewModel.backgroundEventHandler)
    }
    holder.SaveableStateProvider(ScreenStructure.Root::class.toString()) {
        RootNavContent(
            windowInsets = windowInsets,
            current = current,
            rootScreenScaffoldListener = rootScreenScaffoldListener,
            viewModelEventHandlers = viewModelEventHandlers,
            rootCoroutineScope = rootCoroutineScope,
            globalEventSender = globalEventSender,
            globalEvent = globalEvent,
            homeAddTabScreenUiStateProvider = {
                mailScreenViewModel.uiStateFlow.collectAsState().value
            },
            usageCalendarUiStateProvider = { yearMonth ->
                val coroutineScope = rememberCoroutineScope()
                val viewModel = koin.get<ViewModelProviders>()
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
                val viewModel = koin.get<ViewModelProviders>()
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
                val mailImportViewModel = koin.get<ViewModelProviders>()
                    .mailImportViewModel()
                LaunchedEffect(mailImportViewModel.eventHandler) {
                    viewModelEventHandlers.handleMailImport(mailImportViewModel.eventHandler)
                }

                mailImportViewModel.rootUiStateFlow.collectAsState().value
            },
            importMailScreenUiStateProvider = { screenStructure ->
                val importedMailListViewModel = koin.get<ViewModelProviders>()
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
                val allContentViewModel = koin.get<ViewModelProviders>()
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
