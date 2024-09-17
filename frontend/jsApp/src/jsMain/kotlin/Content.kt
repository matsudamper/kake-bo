import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import event.ViewModelEventHandlers
import net.matsudamper.money.frontend.common.base.nav.admin.rememberAdminScreenController
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavControllerImpl
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.MySnackBarHost
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.addmoneyusage.AddMoneyUsageScreen
import net.matsudamper.money.frontend.common.ui.screen.admin.AdminRootScreen
import net.matsudamper.money.frontend.common.ui.screen.importedmail.html.ImportedMailHtmlScreen
import net.matsudamper.money.frontend.common.ui.screen.importedmail.plain.ImportedMailPlainScreen
import net.matsudamper.money.frontend.common.ui.screen.importedmail.root.ImportedMailScreen
import net.matsudamper.money.frontend.common.ui.screen.login.LoginScreen
import net.matsudamper.money.frontend.common.ui.screen.login.LoginScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.moneyusage.MoneyUsageScreen
import net.matsudamper.money.frontend.common.ui.screen.status.NotFoundScreen
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
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
import screen.RootNavContent

@Composable
fun Content(
    modifier: Modifier = Modifier,
    globalEventSender: EventSender<GlobalEvent>,
    composeSizeProvider: () -> MutableStateFlow<IntSize>,
) {
    val rootCoroutineScope = rememberCoroutineScope()
    var hostState by remember { mutableStateOf(SnackbarHostState()) }
    val globalEvent: GlobalEvent =
        remember(hostState, rootCoroutineScope) {
            object : GlobalEvent {
                override fun showSnackBar(message: String) {
                    console.error("show: $message")

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
                    window.alert(message)
                }
            }
        }

    val navController =
        remember {
            ScreenNavControllerImpl(
                initial = RootHomeScreenStructure.Home,
            )
        }
    val loginCheckUseCase =
        remember {
            LoginCheckUseCase(
                ioDispatcher = Dispatchers.Unconfined,
                navController = navController,
                globalEventSender = globalEventSender,
                graphqlQuery = GraphqlUserLoginQuery(),
            )
        }
    val rootViewModel =
        remember {
            RootViewModel(
                loginCheckUseCase = loginCheckUseCase,
                coroutineScope = rootCoroutineScope,
                navController = navController,
            )
        }
    val importedMailListViewModel =
        remember {
            ImportedMailListViewModel(
                coroutineScope = rootCoroutineScope,
                ioDispatcher = Dispatchers.Unconfined,
                graphqlApi = MailLinkScreenGraphqlApi(),
            )
        }
    val mailImportViewModel =
        remember {
            MailImportViewModel(
                coroutineScope = rootCoroutineScope,
                ioDispatcher = Dispatchers.Unconfined,
                graphqlApi = MailImportScreenGraphqlApi(),
                loginCheckUseCase = loginCheckUseCase,
            )
        }

    val rootUsageHostViewModel =
        remember {
            RootUsageHostViewModel(
                coroutineScope = rootCoroutineScope,
                calendarPagingModel =
                RootUsageCalendarPagingModel(
                    coroutineScope = rootCoroutineScope,
                ),
            )
        }
    val mailScreenViewModel =
        remember {
            HomeAddTabScreenViewModel(
                coroutineScope = rootCoroutineScope,
            )
        }
    val settingViewModel =
        remember {
            SettingViewModel(
                coroutineScope = rootCoroutineScope,
                globalEventSender = globalEventSender,
                ioDispatchers = Dispatchers.Unconfined,
            )
        }
    val kakeboScaffoldListener: KakeboScaffoldListener =
        remember {
            object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navController.navigateToHome()
                }
            }
        }
    val rootScreenScaffoldListener: RootScreenScaffoldListener =
        remember(
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

    val viewModelEventHandlers =
        remember(
            navController,
            globalEventSender,
            rootScreenScaffoldListener,
        ) {
            ViewModelEventHandlers(
                navController = navController,
                globalEventSender = globalEventSender,
                rootScreenScaffoldListener = rootScreenScaffoldListener,
            )
        }

    LaunchedEffect(mailScreenViewModel) {
        viewModelEventHandlers.handle(
            mailScreenViewModel.navigateEventHandler,
        )
    }
    LaunchedEffect(
        viewModelEventHandlers,
        rootUsageHostViewModel.rootNavigationEventHandler,
    ) {
        viewModelEventHandlers.handle(
            handler = rootUsageHostViewModel.rootNavigationEventHandler,
        )
    }
    LaunchedEffect(navController.currentNavigation) {
        rootViewModel.navigateChanged()
    }

    Scaffold(
        modifier =
        modifier
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
        val tabHolder = rememberSaveableStateHolder()
        Box(
            modifier =
            Modifier.fillMaxSize()
                .padding(paddingValues),
        ) {
            val holder = rememberSaveableStateHolder()
            when (val current = navController.currentNavigation) {
                is ScreenStructure.Root -> {
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
                        viewModelEventHandlers.handle(settingViewModel.backgroundEventHandler)
                    }
                    holder.SaveableStateProvider(ScreenStructure.Root::class.toString()) {
                        RootNavContent(
                            tabHolder = tabHolder,
                            current = current,
                            rootScreenScaffoldListener = rootScreenScaffoldListener,
                            viewModelEventHandlers = viewModelEventHandlers,
                            rootCoroutineScope = rootCoroutineScope,
                            globalEventSender = globalEventSender,
                            loginCheckUseCase = loginCheckUseCase,
                            globalEvent = globalEvent,
                            homeAddTabScreenUiStateProvider = {
                                mailScreenViewModel.uiStateFlow.collectAsState().value
                            },
                            usageCalendarUiStateProvider = { yearMonth ->
                                val coroutineScope = rememberCoroutineScope()
                                val viewModel =
                                    remember {
                                        MoneyUsagesCalendarViewModel(
                                            coroutineScope = coroutineScope,
                                            rootUsageHostViewModel = rootUsageHostViewModel,
                                            yearMonth = yearMonth,
                                        )
                                    }
                                LaunchedEffect(viewModel.viewModelEventHandler) {
                                    viewModelEventHandlers.handle(
                                        handler = viewModel.viewModelEventHandler,
                                    )
                                }
                                viewModel.uiStateFlow.collectAsState().value
                            },
                            usageListUiStateProvider = {
                                val coroutineScope = rememberCoroutineScope()
                                val viewModel =
                                    remember {
                                        MoneyUsagesListViewModel(
                                            coroutineScope = coroutineScope,
                                            rootUsageHostViewModel = rootUsageHostViewModel,
                                        )
                                    }
                                LaunchedEffect(viewModel.viewModelEventHandler) {
                                    viewModelEventHandlers.handle(
                                        handler = viewModel.viewModelEventHandler,
                                    )
                                }
                                viewModel.uiStateFlow.collectAsState().value
                            },
                            importMailLinkScreenUiStateProvider = {
                                LaunchedEffect(mailImportViewModel.eventHandler) {
                                    viewModelEventHandlers.handle(mailImportViewModel.eventHandler)
                                }

                                mailImportViewModel.rootUiStateFlow.collectAsState().value
                            },
                            importMailScreenUiStateProvider = { screenStructure ->
                                LaunchedEffect(screenStructure) {
                                    importedMailListViewModel.updateQuery(screenStructure)
                                }
                                LaunchedEffect(importedMailListViewModel.eventHandler) {
                                    viewModelEventHandlers.handle(importedMailListViewModel.eventHandler)
                                }
                                importedMailListViewModel.rootUiStateFlow.collectAsState().value
                            },
                            settingUiStateProvider = {
                                settingViewModel.uiState.collectAsState().value
                            },
                        )
                    }
                }

                ScreenStructure.Login -> {
                    val coroutineScope = rememberCoroutineScope()
                    val viewModel =
                        remember {
                            LoginScreenViewModel(
                                coroutineScope = coroutineScope,
                                navController = navController,
                                graphqlQuery = GraphqlUserLoginQuery(),
                                globalEventSender = globalEventSender,
                                screenApi = LoginScreenApi(),
                            )
                        }
                    val uiState: LoginScreenUiState = viewModel.uiStateFlow.collectAsState().value
                    LoginScreen(
                        modifier = Modifier.fillMaxSize(),
                        uiState = uiState,
                    )
                }

                ScreenStructure.Admin -> {
                    val coroutineScope = rememberCoroutineScope()
                    val controller = rememberAdminScreenController()

                    val adminRootViewModel =
                        remember(coroutineScope, controller) {
                            AdminRootScreenViewModel(
                                controller = controller,
                                coroutineScope = coroutineScope,
                                graphqlClient = GlobalContainer.graphqlClient,
                            )
                        }
                    AdminRootScreen(
                        adminScreenController = controller,
                        adminLoginScreenUiStateProvider = {
                            val loginScreenCoroutineScope = rememberCoroutineScope()
                            val loginViewModel =
                                remember(loginScreenCoroutineScope, controller) {
                                    AdminLoginScreenViewModel(
                                        coroutineScope = loginScreenCoroutineScope,
                                        controller = controller,
                                        graphqlClient = GlobalContainer.graphqlClient,
                                    )
                                }
                            loginViewModel.uiStateFlow.collectAsState().value
                        },
                        adminRootScreenUiStateProvider = {
                            adminRootViewModel.uiStateFlow.collectAsState().value
                        },
                        adminAddUserUiStateProvider = {
                            val loginScreenCoroutineScope = rememberCoroutineScope()
                            val adminAddUserScreenViewModel =
                                remember(loginScreenCoroutineScope, controller) {
                                    AdminAddUserScreenViewModel(
                                        coroutineScope = loginScreenCoroutineScope,
                                        controller = controller,
                                        graphqlClient = GlobalContainer.graphqlClient,
                                    )
                                }
                            adminAddUserScreenViewModel.uiStateFlow.collectAsState().value
                        },
                    )
                }

                ScreenStructure.NotFound -> {
                    NotFoundScreen(
                        paddingValues = paddingValues,
                    )
                }

                is ScreenStructure.AddMoneyUsage -> {
                    val viewModel =
                        remember {
                            AddMoneyUsageViewModel(
                                coroutineScope = rootCoroutineScope,
                                graphqlApi = AddMoneyUsageScreenApi(),
                            )
                        }
                    LaunchedEffect(viewModel.eventHandler) {
                        viewModelEventHandlers.handle(
                            handler = viewModel.eventHandler,
                        )
                    }
                    LaunchedEffect(current) {
                        viewModel.updateScreenStructure(current)
                    }
                    AddMoneyUsageScreen(
                        modifier = Modifier.fillMaxSize(),
                        uiState = viewModel.uiStateFlow.collectAsState().value,
                    )
                }

                is ScreenStructure.ImportedMail -> {
                    val coroutineScope = rememberCoroutineScope()
                    val viewModel =
                        remember(
                            coroutineScope,
                            current,
                        ) {
                            ImportedMailScreenViewModel(
                                coroutineScope = coroutineScope,
                                api = ImportedMailScreenGraphqlApi(),
                                importedMailId = current.id,
                            )
                        }
                    LaunchedEffect(viewModel.viewModelEventHandler) {
                        viewModelEventHandlers.handle(
                            handler = viewModel.viewModelEventHandler,
                        )
                    }

                    ImportedMailScreen(
                        modifier = Modifier.fillMaxSize(),
                        uiState = viewModel.uiStateFlow.collectAsState().value,
                    )
                }

                is ScreenStructure.ImportedMailHTML -> {
                    val coroutineScope = rememberCoroutineScope()
                    val viewModel =
                        remember {
                            ImportedMailHtmlViewModel(
                                id = current.id,
                                coroutineScope = coroutineScope,
                            )
                        }
                    LaunchedEffect(viewModel.viewModelEventHandler) {
                        viewModelEventHandlers.handle(viewModel.viewModelEventHandler)
                    }

                    ImportedMailHtmlScreen(
                        modifier = Modifier.fillMaxSize(),
                        uiState = viewModel.uiStateFlow.collectAsState().value,
                        kakeboScaffoldListener = kakeboScaffoldListener,
                    )
                }

                is ScreenStructure.ImportedMailPlain -> {
                    val coroutineScope = rememberCoroutineScope()
                    val viewModel =
                        remember {
                            ImportedMailPlainViewModel(
                                id = current.id,
                                coroutineScope = coroutineScope,
                            )
                        }
                    LaunchedEffect(viewModel.viewModelEventHandler) {
                        viewModelEventHandlers.handle(viewModel.viewModelEventHandler)
                    }

                    ImportedMailPlainScreen(
                        modifier = Modifier.fillMaxSize(),
                        uiState = viewModel.uiStateFlow.collectAsState().value,
                        kakeboScaffoldListener = kakeboScaffoldListener,
                    )
                }

                is ScreenStructure.MoneyUsage -> {
                    val coroutineScope = rememberCoroutineScope()
                    val viewModel =
                        remember {
                            MoneyUsageScreenViewModel(
                                moneyUsageId = current.id,
                                coroutineScope = coroutineScope,
                                api = MoneyUsageScreenViewModelApi(),
                            )
                        }
                    LaunchedEffect(viewModel.eventHandler) {
                        viewModelEventHandlers.handle(viewModel.eventHandler)
                    }
                    MoneyUsageScreen(
                        modifier = Modifier.fillMaxSize(),
                        uiState = viewModel.uiStateFlow.collectAsState().value,
                        kakeboScaffoldListener = kakeboScaffoldListener,
                    )
                }
            }
        }
    }
}
