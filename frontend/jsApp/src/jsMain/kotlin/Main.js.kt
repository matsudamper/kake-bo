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
import net.matsudamper.money.frontend.common.base.Screen
import net.matsudamper.money.frontend.common.base.ScreenNavControllerImpl
import net.matsudamper.money.frontend.common.base.rememberAdminScreenController
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily
import net.matsudamper.money.frontend.common.ui.CustomTheme
import net.matsudamper.money.frontend.common.ui.screen.admin.AdminRootScreen
import net.matsudamper.money.frontend.common.ui.screen.RootRegisterScreen
import net.matsudamper.money.frontend.common.ui.screen.RootScreen
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.RootSettingScreen
import net.matsudamper.money.frontend.common.ui.screen.login.LoginScreen
import net.matsudamper.money.frontend.common.ui.screen.tmp_mail.MailImportScreen
import net.matsudamper.money.frontend.common.uistate.LoginScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
import net.matsudamper.money.frontend.common.viewmodel.LoginScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminAddUserScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminLoginScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminRootScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.MailImportViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.HomeViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.SettingViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.HomeGraphqlApi
import net.matsudamper.money.frontend.graphql.MailImportScreenGraphqlApi
import net.matsudamper.money.frontend.graphql.GraphqlUserConfigQuery
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery
import org.jetbrains.skiko.wasm.onWasmReady

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
            NormalizeInputKeyCapture {
                CustomTheme {
                    val hostState = remember { SnackbarHostState() }
                    val navController = remember {
                        ScreenNavControllerImpl(
                            initial = Screen.Root.Home,
                            directions = Screen.subClass,
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
                                navController.navigate(Screen.Root.Home)
                            }

                            override fun onClickRegister() {
                                navController.navigate(Screen.Root.Register)
                            }

                            override fun onClickSettings() {
                                navController.navigate(Screen.Root.Settings)
                            }
                        }
                    }

                    LaunchedEffect(globalEventSender) {
                        val coroutineScope = this
                        globalEventSender.asHandler().collect(
                            object : GlobalEvent {
                                override fun showSnackBar(message: String) {
                                    console.error("show: $message")
                                    coroutineScope.launch {
                                        hostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short,
                                        )
                                    }
                                }

                                override fun showNativeNotification(message: String) {
                                    window.alert(message)
                                }
                            },
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
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .padding(paddingValues),
                        ) {
                            val rootCoroutineScope = rememberCoroutineScope()
                            when (val current = navController.currentNavigation) {
                                is Screen.Root -> {
                                    val tabHolder = rememberSaveableStateHolder()
                                    when (current) {
                                        Screen.Root.Home -> {
                                            tabHolder.SaveableStateProvider(Screen.Root.Home) {
                                                val viewModel = remember {
                                                    HomeViewModel(
                                                        coroutineScope = rootCoroutineScope,
                                                        homeGraphqlApi = HomeGraphqlApi(),
                                                        loginCheckUseCase = loginCheckUseCase,
                                                    )
                                                }
                                                LaunchedEffect(viewModel.viewModelEventHandler) {
                                                    viewModelEventHandlers.handle(
                                                        handler = viewModel.viewModelEventHandler,
                                                    )
                                                }
                                                RootScreen(
                                                    uiState = viewModel.uiStateFlow.collectAsState().value,
                                                    scaffoldListener = rootScreenScaffoldListener,
                                                )
                                            }
                                        }

                                        Screen.Root.Register -> {
                                            tabHolder.SaveableStateProvider(Screen.Root.Register) {
                                                RootRegisterScreen(
                                                    modifier = Modifier.fillMaxSize(),
                                                    listener = rootScreenScaffoldListener,
                                                )
                                            }
                                        }

                                        Screen.Root.Settings -> {
                                            tabHolder.SaveableStateProvider(Screen.Root.Settings) {
                                                val settingViewModel = remember {
                                                    SettingViewModel(
                                                        coroutineScope = rootCoroutineScope,
                                                        graphqlQuery = GraphqlUserConfigQuery(),
                                                        globalEventSender = globalEventSender,
                                                        ioDispatchers = Dispatchers.Unconfined,
                                                    )
                                                }
                                                RootSettingScreen(
                                                    modifier = Modifier.fillMaxSize(),
                                                    uiState = settingViewModel.uiState.collectAsState().value,
                                                    listener = rootScreenScaffoldListener,
                                                )
                                            }
                                        }
                                    }
                                }

                                Screen.Login -> {
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

                                Screen.Admin -> {
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

                                Screen.MailImport -> {
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
