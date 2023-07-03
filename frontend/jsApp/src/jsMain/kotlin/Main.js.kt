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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import lib.compose.JsCompose
import lib.compose.ResizableComposeWindow
import lib.js.NormalizeInputKeyCapture
import net.matsudamper.money.frontend.common.base.Screen
import net.matsudamper.money.frontend.common.base.ScreenNavControllerImpl
import net.matsudamper.money.frontend.common.base.rememberAdminScreenController
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily
import net.matsudamper.money.frontend.common.ui.CustomTheme
import net.matsudamper.money.frontend.common.ui.layout.AdminRootScreen
import net.matsudamper.money.frontend.common.ui.screen.RootRegisterScreen
import net.matsudamper.money.frontend.common.ui.screen.RootScreen
import net.matsudamper.money.frontend.common.ui.screen.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.RootSettingScreen
import net.matsudamper.money.frontend.common.ui.screen.login.LoginScreen
import net.matsudamper.money.frontend.common.uistate.LoginScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.LoginScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminAddUserScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminLoginScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminRootScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.HomeViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.SettingViewModel
import net.matsudamper.money.frontend.graphql.GraphqlMailQuery
import net.matsudamper.money.frontend.graphql.GraphqlUserConfigQuery
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery
import org.jetbrains.skiko.SkikoKey
import org.jetbrains.skiko.wasm.onWasmReady

@OptIn(ExperimentalMaterial3Api::class)
fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {
    val composeSize = MutableStateFlow(IntSize.Zero)

    JsCompose(
        composeSize = composeSize,
    )

    onWasmReady {
        val globalEventSender = EventSender<GlobalEvent>()
        ResizableComposeWindow(
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
                        globalEventSender.asReceiver().collect(
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
                            when (val current = navController.currentNavigation) {
                                is Screen.Root -> {
                                    val rootCoroutineScope = rememberCoroutineScope()
                                    val homeViewModel = remember {
                                        HomeViewModel(
                                            coroutineScope = rootCoroutineScope,
                                            ioDispatcher = Dispatchers.Unconfined,
                                            mailQuery = GraphqlMailQuery(),
                                            graphqlQuery = GraphqlUserLoginQuery(),
                                            navController = navController,
                                            globalEventSender = globalEventSender,
                                        )
                                    }

                                    val settingViewModel = remember {
                                        SettingViewModel(
                                            coroutineScope = rootCoroutineScope,
                                            graphqlQuery = GraphqlUserConfigQuery(),
                                            globalEventSender = globalEventSender,
                                            ioDispatchers = Dispatchers.Unconfined,
                                        )
                                    }
                                    LaunchedEffect(Unit) {
                                        homeViewModel.onResume()
                                    }
                                    val homeUiState = homeViewModel.rootUiStateFlow.collectAsState().value
                                    when (current) {
                                        Screen.Root.Home -> {
                                            RootScreen(
                                                uiState = homeUiState,
                                                listener = rootScreenScaffoldListener,
                                            )
                                        }

                                        Screen.Root.Register -> {
                                            RootRegisterScreen(
                                                modifier = Modifier.fillMaxSize(),
                                                listener = rootScreenScaffoldListener,
                                            )
                                        }

                                        Screen.Root.Settings -> {
                                            RootSettingScreen(
                                                modifier = Modifier.fillMaxSize(),
                                                uiState = settingViewModel.uiState.collectAsState().value,
                                                listener = rootScreenScaffoldListener,
                                            )
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
