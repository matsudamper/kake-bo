package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.SettingViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabPeriodAllContentViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabScreenApi
import net.matsudamper.money.frontend.common.viewmodel.root.mail.HomeAddTabScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.mail.ImportedMailListViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.mail.MailImportViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.MoneyUsagesCalendarViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.MoneyUsagesListViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.RootUsageHostViewModel
import net.matsudamper.money.frontend.graphql.MailImportScreenGraphqlApi
import net.matsudamper.money.frontend.graphql.MailLinkScreenGraphqlApi

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
            tabHolder = holder,
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
                val viewModel = remember {
                    MoneyUsagesCalendarViewModel(
                        coroutineScope = coroutineScope,
                        rootUsageHostViewModel = rootUsageHostViewModel,
                        yearMonth = yearMonth,
                    )
                }
                LaunchedEffect(viewModel.viewModelEventHandler) {
                    viewModelEventHandlers.handleMoneyUsagesCalendar(
                        handler = viewModel.viewModelEventHandler,
                    )
                }
                viewModel.uiStateFlow.collectAsState().value
            },
            usageListUiStateProvider = {
                val coroutineScope = rememberCoroutineScope()
                val viewModel = remember {
                    MoneyUsagesListViewModel(
                        coroutineScope = coroutineScope,
                        rootUsageHostViewModel = rootUsageHostViewModel,
                        graphqlClient = koin.get(),
                    )
                }
                LaunchedEffect(viewModel.viewModelEventHandler) {
                    viewModelEventHandlers.handleMoneyUsagesList(
                        handler = viewModel.viewModelEventHandler,
                    )
                }
                viewModel.uiStateFlow.collectAsState().value
            },
            importMailLinkScreenUiStateProvider = {
                val mailImportViewModel = remember {
                    MailImportViewModel(
                        coroutineScope = rootCoroutineScope,
                        ioDispatcher = Dispatchers.IO,
                        graphqlApi = MailImportScreenGraphqlApi(
                            graphqlClient = koin.get(),
                        ),
                        loginCheckUseCase = koin.get(),
                    )
                }
                LaunchedEffect(mailImportViewModel.eventHandler) {
                    viewModelEventHandlers.handleMailImport(mailImportViewModel.eventHandler)
                }

                mailImportViewModel.rootUiStateFlow.collectAsState().value
            },
            importMailScreenUiStateProvider = { screenStructure ->
                val importedMailListViewModel = remember {
                    ImportedMailListViewModel(
                        coroutineScope = rootCoroutineScope,
                        ioDispatcher = Dispatchers.IO,
                        graphqlApi = MailLinkScreenGraphqlApi(
                            graphqlClient = koin.get(),
                        ),
                    )
                }
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
                val allContentViewModel = remember {
                    RootHomeTabPeriodAllContentViewModel(
                        coroutineScope = rootCoroutineScope,
                        api = RootHomeTabScreenApi(
                            graphqlClient = koin.get(),
                        ),
                        loginCheckUseCase = koin.get(),
                        graphqlClient = koin.get(),
                    )
                }
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
