import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarDefaults.actionColor
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import event.ViewModelEventHandlers
import lib.compose.JsCompose
import lib.js.NormalizeInputKeyCapture
import net.matsudamper.money.frontend.common.base.ScreenNavControllerImpl
import net.matsudamper.money.frontend.common.base.nav.admin.rememberAdminScreenController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily
import net.matsudamper.money.frontend.common.ui.CustomTheme
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.add_money_usage.AddMoneyUsageScreen
import net.matsudamper.money.frontend.common.ui.screen.admin.AdminRootScreen
import net.matsudamper.money.frontend.common.ui.screen.login.LoginScreen
import net.matsudamper.money.frontend.common.ui.screen.status.NotFoundScreen
import net.matsudamper.money.frontend.common.ui.screen.tmp_mail.MailImportScreen
import net.matsudamper.money.frontend.common.ui.screen.tmp_mail.MailLinkScreen
import net.matsudamper.money.frontend.common.uistate.LoginScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
import net.matsudamper.money.frontend.common.viewmodel.LoginScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.add_money_usage.AddMoneyUsageViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminAddUserScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminLoginScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminRootScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.MailImportViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.MailLinkViewModel
import net.matsudamper.money.frontend.common.viewmodel.add_money_usage.AddMoneyUsageScreenApi
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery
import net.matsudamper.money.frontend.graphql.MailImportScreenGraphqlApi
import net.matsudamper.money.frontend.graphql.MailLinkScreenGraphqlApi
import org.jetbrains.skiko.wasm.onWasmReady
import screen.RootNavContent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {
    val composeSize = MutableStateFlow(IntSize.Zero)
    JsCompose(
        composeSize = composeSize,
    )

    onWasmReady {
        val globalEventSender = EventSender<GlobalEvent>()
        CanvasBasedWindow(
            title = "家計簿",
        ) {
            val rootCoroutineScope = rememberCoroutineScope()

            NormalizeInputKeyCapture {
                CustomTheme {
                    val hostState = remember { SnackbarHostState() }
                    val globalEvent: GlobalEvent = remember(hostState, rootCoroutineScope) {
                        object : GlobalEvent {
                            override fun showSnackBar(message: String) {
                                console.error("show: $message")
                                rootCoroutineScope.launch {
                                    hostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short,
                                    )
                                }
                            }

                            override fun showNativeNotification(message: String) {
                                window.alert(message)
                            }
                        }
                    }

                    val navController = remember {
                        ScreenNavControllerImpl(
                            initial = ScreenStructure.Root.Home(),
                        )
                    }
                    val loginCheckUseCase = remember {
                        LoginCheckUseCase(
                            ioDispatcher = Dispatchers.Unconfined,
                            navController = navController,
                            globalEventSender = globalEventSender,
                            graphqlQuery = GraphqlUserLoginQuery(),
                        )
                    }
                    val rootScreenScaffoldListener: RootScreenScaffoldListener = remember(navController) {
                        object : RootScreenScaffoldListener {
                            override fun onClickHome() {
                                navController.navigate(ScreenStructure.Root.Home())
                            }

                            override fun onClickRegister() {
                                navController.navigate(ScreenStructure.Root.List())
                            }

                            override fun onClickSettings() {
                                navController.navigate(ScreenStructure.Root.Settings.Root)
                            }

                            override fun onClickMail() {
                                navController.navigate(ScreenStructure.Root.MailList(isLinked = false)) // TODO
                            }
                        }
                    }

                    LaunchedEffect(globalEventSender) {
                        globalEventSender.asHandler().collect(
                            globalEvent,
                        )
                    }

                    val viewModelEventHandlers = remember(navController) {
                        ViewModelEventHandlers(
                            navController = navController,
                            globalEventSender = globalEventSender,
                        )
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged {
                                composeSize.value = it
                            },
                        snackbarHost = {
                            MySnackBarHost(
                                hostState = hostState,
                            )
                        },
                    ) { paddingValues ->
                        val tabHolder = rememberSaveableStateHolder()
                        val mailLinkViewModel = remember {
                            MailLinkViewModel(
                                coroutineScope = rootCoroutineScope,
                                ioDispatcher = Dispatchers.Unconfined,
                                graphqlApi = MailLinkScreenGraphqlApi(),
                            )
                        }
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .padding(paddingValues),
                        ) {
                            when (val current = navController.currentNavigation) {
                                is ScreenStructure.Root -> {
                                    RootNavContent(
                                        tabHolder = tabHolder,
                                        current = current,
                                        rootScreenScaffoldListener = rootScreenScaffoldListener,
                                        viewModelEventHandlers = viewModelEventHandlers,
                                        rootCoroutineScope = rootCoroutineScope,
                                        globalEventSender = globalEventSender,
                                        loginCheckUseCase = loginCheckUseCase,
                                        globalEvent = globalEvent,
                                        mailListUiStateProvider = { screenStructure ->
                                            LaunchedEffect(screenStructure) {
                                                mailLinkViewModel.updateQuery(screenStructure)
                                            }
                                            LaunchedEffect(mailLinkViewModel.eventHandler) {
                                                viewModelEventHandlers.handle(mailLinkViewModel.eventHandler)
                                            }
                                            mailLinkViewModel.rootUiStateFlow.collectAsState().value
                                        },
                                    )
                                }

                                ScreenStructure.Login -> {
                                    val coroutineScope = rememberCoroutineScope()
                                    val viewModel = remember {
                                        LoginScreenViewModel(
                                            coroutineScope = coroutineScope,
                                            navController = navController,
                                            graphqlQuery = GraphqlUserLoginQuery(),
                                            globalEventSender = globalEventSender,
                                        )
                                    }
                                    val uiState: LoginScreenUiState = viewModel.uiStateFlow.collectAsState().value
                                    LoginScreen(
                                        uiState = uiState,
                                    )
                                }

                                ScreenStructure.Admin -> {
                                    val coroutineScope = rememberCoroutineScope()
                                    val controller = rememberAdminScreenController()

                                    val rootViewModel = remember(coroutineScope, controller) {
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
                                            val loginViewModel = remember(loginScreenCoroutineScope, controller) {
                                                AdminLoginScreenViewModel(
                                                    coroutineScope = loginScreenCoroutineScope,
                                                    controller = controller,
                                                    graphqlClient = GlobalContainer.graphqlClient,
                                                )
                                            }
                                            loginViewModel.uiStateFlow.collectAsState().value
                                        },
                                        adminRootScreenUiStateProvider = {
                                            rootViewModel.uiStateFlow.collectAsState().value
                                        },
                                        adminAddUserUiStateProvider = {
                                            val loginScreenCoroutineScope = rememberCoroutineScope()
                                            val adminAddUserScreenViewModel = remember(loginScreenCoroutineScope, controller) {
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

                                ScreenStructure.MailImport -> {
                                    val mailImportViewModel = remember {
                                        MailImportViewModel(
                                            coroutineScope = rootCoroutineScope,
                                            ioDispatcher = Dispatchers.Unconfined,
                                            graphqlApi = MailImportScreenGraphqlApi(),
                                            loginCheckUseCase = loginCheckUseCase,
                                        )
                                    }

                                    LaunchedEffect(mailImportViewModel.eventHandler) {
                                        viewModelEventHandlers.handle(mailImportViewModel.eventHandler)
                                    }

                                    MailImportScreen(
                                        uiState = mailImportViewModel.rootUiStateFlow.collectAsState().value,
                                    )
                                }

                                ScreenStructure.NotFound -> {
                                    NotFoundScreen(
                                        paddingValues = paddingValues,
                                    )
                                }

                                ScreenStructure.AddMoneyUsage -> {
                                    val viewModel = remember {
                                        AddMoneyUsageViewModel(
                                            coroutineScope = rootCoroutineScope,
                                            graphqlApi = AddMoneyUsageScreenApi(),
                                        )
                                    }
                                    AddMoneyUsageScreen(
                                        modifier = Modifier.fillMaxSize(),
                                        uiState = viewModel.uiStateFlow.collectAsState().value,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MySnackBarHost(
    modifier: Modifier = Modifier,
    hostState: SnackbarHostState,
) {
    SnackbarHost(
        modifier = modifier,
        hostState = hostState,
    ) { snackbarData ->
        val actionLabel = snackbarData.visuals.actionLabel
        Snackbar(
            modifier = Modifier.padding(12.dp),
            action = if (actionLabel != null) {
                {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = actionColor),
                        onClick = { snackbarData.performAction() },
                        content = {
                            Text(
                                text = actionLabel,
                                fontFamily = rememberCustomFontFamily(),
                            )
                        },
                    )
                }
            } else {
                null
            },
            dismissAction = if (snackbarData.visuals.withDismissAction) {
                {
                    IconButton(
                        onClick = { snackbarData.dismiss() },
                        content = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = null,
                            )
                        },
                    )
                }
            } else {
                null
            },
            actionOnNewLine = false,
            shape = SnackbarDefaults.shape,
            containerColor = SnackbarDefaults.color,
            contentColor = SnackbarDefaults.contentColor,
            actionContentColor = SnackbarDefaults.actionContentColor,
            dismissActionContentColor = SnackbarDefaults.dismissActionContentColor,
            content = {
                Text(
                    text = snackbarData.visuals.message,
                    fontFamily = rememberCustomFontFamily(),
                )
            },
        )
    }
}
