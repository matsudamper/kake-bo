package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.admin.rememberAdminScreenController
import net.matsudamper.money.frontend.common.base.nav.user.JsScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavControllerImpl
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.MySnackBarHost
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.layout.BackHandler
import net.matsudamper.money.frontend.common.ui.screen.addmoneyusage.AddMoneyUsageScreen
import net.matsudamper.money.frontend.common.ui.screen.admin.AdminRootScreen
import net.matsudamper.money.frontend.common.ui.screen.importedmail.html.ImportedMailHtmlScreen
import net.matsudamper.money.frontend.common.ui.screen.importedmail.plain.ImportedMailPlainScreen
import net.matsudamper.money.frontend.common.ui.screen.importedmail.root.ImportedMailScreen
import net.matsudamper.money.frontend.common.ui.screen.login.LoginScreen
import net.matsudamper.money.frontend.common.ui.screen.login.LoginScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.moneyusage.MoneyUsageScreen
import net.matsudamper.money.frontend.common.ui.screen.status.NotFoundScreen
import net.matsudamper.money.frontend.common.usecase.LoginCheckUseCaseImpl
import net.matsudamper.money.frontend.common.viewmodel.GlobalEventHandlerLoginCheckUseCaseDelegate
import net.matsudamper.money.frontend.common.viewmodel.LoginScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.addmoneyusage.AddMoneyUsageScreenApi
import net.matsudamper.money.frontend.common.viewmodel.addmoneyusage.AddMoneyUsageViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminAddUserScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminLoginScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminRootScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.importedmail.html.ImportedMailHtmlViewModel
import net.matsudamper.money.frontend.common.viewmodel.importedmail.plain.ImportedMailPlainViewModel
import net.matsudamper.money.frontend.common.viewmodel.importedmail.root.ImportedMailScreenGraphqlApi
import net.matsudamper.money.frontend.common.viewmodel.importedmail.root.ImportedMailScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.login.LoginScreenApi
import net.matsudamper.money.frontend.common.viewmodel.moneyusage.MoneyUsageScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.moneyusage.MoneyUsageScreenViewModelApi
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.RootViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.SettingViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.LoginCheckUseCaseEventListenerImpl
import net.matsudamper.money.frontend.common.viewmodel.root.mail.HomeAddTabScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.mail.ImportedMailListViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.mail.MailImportViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.MoneyUsagesCalendarViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.MoneyUsagesListViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.RootUsageCalendarPagingModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.RootUsageHostViewModel
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery
import net.matsudamper.money.frontend.graphql.MailImportScreenGraphqlApi
import net.matsudamper.money.frontend.graphql.MailLinkScreenGraphqlApi
import net.matsudamper.money.ui.root.platform.rememberUrlOpener
import org.koin.dsl.module

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Content(
    modifier: Modifier = Modifier,
    globalEventSender: EventSender<GlobalEvent>,
    composeSizeProvider: () -> MutableStateFlow<IntSize> = { MutableStateFlow(IntSize.Zero) },
) {
    val koin = LocalKoin.current
    var alertDialogInfo: String? by remember { mutableStateOf(null) }
    val rootCoroutineScope = rememberCoroutineScope()
    var hostState by remember { mutableStateOf(SnackbarHostState()) }
    val globalEvent: GlobalEvent = remember(hostState, rootCoroutineScope) {
        object : GlobalEvent {
            override fun showSnackBar(message: String) {
                // 二回目が表示されないのでstate自体を作成し直す
                hostState = SnackbarHostState()
                rootCoroutineScope.launch {
                    hostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short,
                        withDismissAction = true,
                    )
                }
            }

            override fun showNativeNotification(message: String) {
                alertDialogInfo = message
            }
        }
    }
    run {
        val nonNullAlertDialogInfo = alertDialogInfo ?: return@run

        BasicAlertDialog(
            onDismissRequest = {
                alertDialogInfo = null
            },
        ) {
            Text(nonNullAlertDialogInfo)
        }
    }

    val navController = remember {
        ScreenNavControllerImpl(
            initial = RootHomeScreenStructure.Home,
        )
    }

    remember(Unit) {
        val modules = listOf(
            module {
                factory<GlobalEventHandlerLoginCheckUseCaseDelegate> {
                    GlobalEventHandlerLoginCheckUseCaseDelegate(
                        useCase = LoginCheckUseCaseImpl(
                            graphqlQuery = GraphqlUserLoginQuery(
                                graphqlClient = get(),
                            ),
                            eventListener = LoginCheckUseCaseEventListenerImpl(
                                navController = navController,
                                globalEventSender = globalEventSender,
                                coroutineScope = rootCoroutineScope,
                            ),
                        ),
                    )
                }
            },
        )
        koin.loadModules(modules)
        object : RememberObserver {
            override fun onAbandoned() {}
            override fun onRemembered() {
            }

            override fun onForgotten() {
                koin.unloadModules(modules)
            }
        }
    }

    val rootViewModel = remember {
        RootViewModel(
            loginCheckUseCase = koin.get(),
            coroutineScope = rootCoroutineScope,
            navController = navController,
        )
    }
    val importedMailListViewModel = remember {
        ImportedMailListViewModel(
            coroutineScope = rootCoroutineScope,
            ioDispatcher = Dispatchers.Unconfined,
            graphqlApi = MailLinkScreenGraphqlApi(
                graphqlClient = koin.get(),
            ),
        )
    }
    val mailImportViewModel = remember {
        MailImportViewModel(
            coroutineScope = rootCoroutineScope,
            ioDispatcher = Dispatchers.Unconfined,
            graphqlApi = MailImportScreenGraphqlApi(
                graphqlClient = koin.get(),
            ),
            loginCheckUseCase = koin.get(),
        )
    }

    val rootUsageHostViewModel = remember {
        RootUsageHostViewModel(
            coroutineScope = rootCoroutineScope,
            calendarPagingModel =
            RootUsageCalendarPagingModel(
                coroutineScope = rootCoroutineScope,
                graphqlClient = koin.get(),
            ),
        )
    }
    val mailScreenViewModel = remember {
        HomeAddTabScreenViewModel(
            coroutineScope = rootCoroutineScope,
        )
    }
    val settingViewModel = remember {
        SettingViewModel(
            coroutineScope = rootCoroutineScope,
            globalEventSender = globalEventSender,
            ioDispatchers = Dispatchers.Unconfined,
        )
    }
    val kakeboScaffoldListener: KakeboScaffoldListener = remember {
        object : KakeboScaffoldListener {
            override fun onClickTitle() {
                navController.navigateToHome()
            }
        }
    }
    val rootScreenScaffoldListener: RootScreenScaffoldListener = remember(
        navController,
        mailScreenViewModel,
    ) {
        object : RootScreenScaffoldListener {
            override val kakeboScaffoldListener: KakeboScaffoldListener = kakeboScaffoldListener

            override fun onClickHome() {
                navController.navigate(RootHomeScreenStructure.Home)
            }

            override fun onClickList() {
                rootUsageHostViewModel.requestNavigate()
            }

            override fun onClickSettings() {
                settingViewModel.requestNavigate(
                    currentScreen = navController.currentNavigation,
                )
            }

            override fun onClickAdd() {
                mailScreenViewModel.requestNavigate()
            }
        }
    }

    LaunchedEffect(globalEventSender, globalEvent) {
        globalEventSender.asHandler().collect(
            globalEvent,
        )
    }

    val urlOpener = rememberUrlOpener()
    val viewModelEventHandlers = remember(
        navController,
        globalEventSender,
        rootScreenScaffoldListener,
        urlOpener,
    ) {
        ViewModelEventHandlers(
            navController = navController,
            globalEventSender = globalEventSender,
            rootScreenScaffoldListener = rootScreenScaffoldListener,
            urlOpener = urlOpener,
        )
    }
    remember {
        object : RememberObserver {
            override fun onAbandoned() {}
            override fun onRemembered() {
            }

            override fun onForgotten() {
            }
        }
    }

    LaunchedEffect(mailScreenViewModel) {
        viewModelEventHandlers.handleHomeAddTabScreen(
            mailScreenViewModel.navigateEventHandler,
        )
    }
    LaunchedEffect(
        viewModelEventHandlers,
        rootUsageHostViewModel.rootNavigationEventHandler,
    ) {
        viewModelEventHandlers.handleRootUsageHost(
            handler = rootUsageHostViewModel.rootNavigationEventHandler,
        )
    }
    LaunchedEffect(navController.currentNavigation) {
        rootViewModel.navigateChanged()
    }
    BackHandler(true) {
        navController.back()
    }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                composeSizeProvider().value = it
            },
        snackbarHost = {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                MySnackBarHost(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    hostState = hostState,
                )
            }
        },
    ) { paddingValues ->
        val rootHolder = rememberSaveableStateHolder()
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val current = navController.currentNavigation) {
                is ScreenStructure.Root -> {
                    RootScreenContainer(
                        current = current,
                        settingViewModel = settingViewModel,
                        mailScreenViewModel = mailScreenViewModel,
                        rootUsageHostViewModel = rootUsageHostViewModel,
                        viewModelEventHandlers = viewModelEventHandlers,
                        mailImportViewModel = mailImportViewModel,
                        importedMailListViewModel = importedMailListViewModel,
                        holder = rootHolder,
                        rootScreenScaffoldListener = rootScreenScaffoldListener,
                        rootCoroutineScope = rootCoroutineScope,
                        globalEventSender = globalEventSender,
                        globalEvent = globalEvent,
                        windowInsets = paddingValues,
                    )
                }

                ScreenStructure.Login -> {
                    LoginScreenContainer(
                        navController = navController,
                        globalEventSender = globalEventSender,
                        windowInsets = paddingValues,
                    )
                }

                ScreenStructure.Admin -> {
                    AdminContainer(
                        windowInsets = paddingValues,
                    )
                }

                ScreenStructure.NotFound -> {
                    NotFoundScreen(
                        paddingValues = paddingValues,
                    )
                }

                is ScreenStructure.AddMoneyUsage -> {
                    AddMoneyUsageScreenContainer(
                        rootCoroutineScope = rootCoroutineScope,
                        current = current,
                        viewModelEventHandlers = viewModelEventHandlers,
                        windowInsets = paddingValues,
                    )
                }

                is ScreenStructure.ImportedMail -> {
                    ImportedMailScreenContainer(
                        current = current,
                        viewModelEventHandlers = viewModelEventHandlers,
                        windowInsets = paddingValues,
                    )
                }

                is ScreenStructure.ImportedMailHTML -> {
                    ImportedMailHtmlContainer(
                        current = current,
                        viewModelEventHandlers = viewModelEventHandlers,
                        kakeboScaffoldListener = kakeboScaffoldListener,
                        windowInsets = paddingValues,
                    )
                }

                is ScreenStructure.ImportedMailPlain -> {
                    ImportedMailPlainScreenContainer(
                        screen = current,
                        viewModelEventHandlers = viewModelEventHandlers,
                        kakeboScaffoldListener = kakeboScaffoldListener,
                        windowInsets = paddingValues,
                    )
                }

                is ScreenStructure.MoneyUsage -> {
                    MoneyUsageContainer(
                        screen = current,
                        viewModelEventHandlers = viewModelEventHandlers,
                        kakeboScaffoldListener = kakeboScaffoldListener,
                        windowInsets = paddingValues,
                    )
                }
            }
        }
    }
}

@Composable
private fun RootScreenContainer(
    current: ScreenStructure.Root,
    settingViewModel: SettingViewModel,
    mailScreenViewModel: HomeAddTabScreenViewModel,
    rootUsageHostViewModel: RootUsageHostViewModel,
    viewModelEventHandlers: ViewModelEventHandlers,
    mailImportViewModel: MailImportViewModel,
    importedMailListViewModel: ImportedMailListViewModel,
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
                LaunchedEffect(mailImportViewModel.eventHandler) {
                    viewModelEventHandlers.handleMailImport(mailImportViewModel.eventHandler)
                }

                mailImportViewModel.rootUiStateFlow.collectAsState().value
            },
            importMailScreenUiStateProvider = { screenStructure ->
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
        )
    }
}

@Composable
private fun AddMoneyUsageScreenContainer(
    current: ScreenStructure.AddMoneyUsage,
    viewModelEventHandlers: ViewModelEventHandlers,
    rootCoroutineScope: CoroutineScope,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val viewModel = remember {
        AddMoneyUsageViewModel(
            coroutineScope = rootCoroutineScope,
            graphqlApi = AddMoneyUsageScreenApi(
                graphqlClient = koin.get(),
            ),
            graphqlClient = koin.get(),
        )
    }
    LaunchedEffect(viewModel.eventHandler) {
        viewModelEventHandlers.handleAddMoneyUsage(
            handler = viewModel.eventHandler,
        )
    }
    LaunchedEffect(current) {
        viewModel.updateScreenStructure(current)
    }
    AddMoneyUsageScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = viewModel.uiStateFlow.collectAsState().value,
        windowInsets = windowInsets,
    )
}

@Composable
private fun ImportedMailScreenContainer(
    current: ScreenStructure.ImportedMail,
    viewModelEventHandlers: ViewModelEventHandlers,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel = remember(
        coroutineScope,
        current,
    ) {
        ImportedMailScreenViewModel(
            coroutineScope = coroutineScope,
            api = ImportedMailScreenGraphqlApi(
                graphqlClient = koin.get(),
            ),
            importedMailId = current.id,
        )
    }
    LaunchedEffect(viewModel.viewModelEventHandler) {
        viewModelEventHandlers.handleImportedMailScreen(
            handler = viewModel.viewModelEventHandler,
        )
    }

    ImportedMailScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = viewModel.uiStateFlow.collectAsState().value,
        windowInsets = windowInsets,
    )
}

@Composable
private fun ImportedMailHtmlContainer(
    current: ScreenStructure.ImportedMailHTML,
    viewModelEventHandlers: ViewModelEventHandlers,
    kakeboScaffoldListener: KakeboScaffoldListener,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel = remember {
        ImportedMailHtmlViewModel(
            id = current.id,
            coroutineScope = coroutineScope,
            graphqlClient = koin.get(),
        )
    }
    LaunchedEffect(viewModel.viewModelEventHandler) {
        viewModelEventHandlers.handleImportedMailHtml(viewModel.viewModelEventHandler)
    }

    ImportedMailHtmlScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = viewModel.uiStateFlow.collectAsState().value,
        kakeboScaffoldListener = kakeboScaffoldListener,
        windowInsets = windowInsets,
    )
}

@Composable
private fun ImportedMailPlainScreenContainer(
    screen: ScreenStructure.ImportedMailPlain,
    viewModelEventHandlers: ViewModelEventHandlers,
    kakeboScaffoldListener: KakeboScaffoldListener,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel = remember {
        ImportedMailPlainViewModel(
            id = screen.id,
            coroutineScope = coroutineScope,
            graphqlClient = koin.get(),
        )
    }
    LaunchedEffect(viewModel.viewModelEventHandler) {
        viewModelEventHandlers.handleImportedMailPlain(viewModel.viewModelEventHandler)
    }

    ImportedMailPlainScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = viewModel.uiStateFlow.collectAsState().value,
        kakeboScaffoldListener = kakeboScaffoldListener,
        windowInsets = windowInsets,
    )
}

@Composable
private fun MoneyUsageContainer(
    screen: ScreenStructure.MoneyUsage,
    viewModelEventHandlers: ViewModelEventHandlers,
    kakeboScaffoldListener: KakeboScaffoldListener,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel = remember {
        MoneyUsageScreenViewModel(
            moneyUsageId = screen.id,
            coroutineScope = coroutineScope,
            api = MoneyUsageScreenViewModelApi(
                graphqlClient = koin.get(),
            ),
            graphqlClient = koin.get(),
        )
    }
    LaunchedEffect(viewModel.eventHandler) {
        viewModelEventHandlers.handleMoneyUsageScreen(viewModel.eventHandler)
    }
    MoneyUsageScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = viewModel.uiStateFlow.collectAsState().value,
        kakeboScaffoldListener = kakeboScaffoldListener,
        windowInsets = windowInsets,
    )
}

@Composable
private fun LoginScreenContainer(
    navController: JsScreenNavController,
    globalEventSender: EventSender<GlobalEvent>,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel = remember {
        LoginScreenViewModel(
            coroutineScope = coroutineScope,
            navController = navController,
            graphqlQuery = GraphqlUserLoginQuery(
                graphqlClient = koin.get(),
            ),
            globalEventSender = globalEventSender,
            screenApi = LoginScreenApi(
                graphqlClient = koin.get(),
            ),
            webAuthModel = koin.get(),
        )
    }
    val uiState: LoginScreenUiState = viewModel.uiStateFlow.collectAsState().value
    LoginScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = uiState,
        windowInsets = windowInsets,
    )
}

@Composable
private fun AdminContainer(
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val coroutineScope = rememberCoroutineScope()
    val controller = rememberAdminScreenController()

    val adminRootViewModel = remember(coroutineScope, controller) {
        AdminRootScreenViewModel(
            controller = controller,
            coroutineScope = coroutineScope,
            graphqlClient = koin.get(),
        )
    }
    AdminRootScreen(
        adminScreenController = controller,
        adminLoginScreenUiStateProvider = {
            val loginScreenCoroutineScope = rememberCoroutineScope()
            val loginViewModel = remember(loginScreenCoroutineScope, controller) {
                AdminLoginScreenViewModel(
                    coroutineScope = loginScreenCoroutineScope,
                    controller = controller,
                    graphqlClient = koin.get(),
                )
            }
            loginViewModel.uiStateFlow.collectAsState().value
        },
        adminRootScreenUiStateProvider = {
            adminRootViewModel.uiStateFlow.collectAsState().value
        },
        adminAddUserUiStateProvider = {
            val loginScreenCoroutineScope = rememberCoroutineScope()
            val adminAddUserScreenViewModel = remember(loginScreenCoroutineScope, controller) {
                AdminAddUserScreenViewModel(
                    coroutineScope = loginScreenCoroutineScope,
                    controller = controller,
                    graphqlClient = koin.get(),
                )
            }
            adminAddUserScreenViewModel.uiStateFlow.collectAsState().value
        },
        windowInsets = windowInsets,
    )
}
