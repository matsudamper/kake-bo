package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.Box
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.nav.user.IScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.MySnackBarHost
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.status.NotFoundScreen
import net.matsudamper.money.frontend.common.usecase.LoginCheckUseCaseImpl
import net.matsudamper.money.frontend.common.viewmodel.GlobalEventHandlerLoginCheckUseCaseDelegate
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.SettingViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.LoginCheckUseCaseEventListenerImpl
import net.matsudamper.money.frontend.common.viewmodel.root.mail.HomeAddTabScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.RootUsageCalendarPagingModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.RootUsageHostViewModel
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery
import net.matsudamper.money.ui.root.platform.PlatformTools
import net.matsudamper.money.ui.root.viewmodel.ViewModelProviders
import net.matsudamper.money.ui.root.viewmodel.provideViewModel
import org.koin.dsl.module

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun Content(
    modifier: Modifier = Modifier,
    globalEventSender: EventSender<GlobalEvent>,
    platformToolsProvider: () -> PlatformTools,
    navController: ScreenNavController<IScreenStructure>,
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
                factory<ViewModelProviders> {
                    ViewModelProviders(
                        koin = this.getKoin(),
                        navController = navController,
                        rootCoroutineScope = rootCoroutineScope,
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

    val rootViewModel = koin.get<ViewModelProviders>().rootViewModel()

    val rootUsageHostViewModel = provideViewModel {
        RootUsageHostViewModel(
            viewModelFeature = it,
            calendarPagingModel = RootUsageCalendarPagingModel(
                coroutineScope = rootCoroutineScope,
                graphqlClient = koin.get(),
            ),
        )
    }
    val mailScreenViewModel = provideViewModel {
        HomeAddTabScreenViewModel(
            viewModelFeature = it,
        )
    }
    val settingViewModel = provideViewModel {
        SettingViewModel(
            viewModelFeature = it,
            globalEventSender = globalEventSender,
            ioDispatchers = Dispatchers.IO,
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
                Logger.d("LOG", "onclickList")
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

    val viewModelEventHandlers = remember(
        navController,
        globalEventSender,
        rootScreenScaffoldListener,
        platformToolsProvider,
    ) {
        ViewModelEventHandlers(
            navController = navController,
            globalEventSender = globalEventSender,
            rootScreenScaffoldListener = rootScreenScaffoldListener,
            platformToolsProvider = platformToolsProvider,
        )
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
                is ScreenStructure -> {
                    when (current) {
                        is ScreenStructure.Root -> {
                            RootScreenContainer(
                                current = current,
                                settingViewModel = settingViewModel,
                                mailScreenViewModel = mailScreenViewModel,
                                rootUsageHostViewModel = rootUsageHostViewModel,
                                viewModelEventHandlers = viewModelEventHandlers,
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
                else -> throw NotImplementedError("$current is not implemented")
            }
        }
    }
}
