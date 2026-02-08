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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.NavHost
import net.matsudamper.money.frontend.common.base.nav.rememberScopedObjectStoreOwner
import net.matsudamper.money.frontend.common.base.nav.user.IScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.MySnackBarHost
import net.matsudamper.money.frontend.common.ui.screen.status.NotFoundScreen
import net.matsudamper.money.frontend.common.usecase.LoginCheckUseCaseImpl
import net.matsudamper.money.frontend.common.viewmodel.GlobalEventHandlerLoginCheckUseCaseDelegate
import net.matsudamper.money.frontend.common.viewmodel.LocalGlobalEventHandlerLoginCheckUseCaseDelegate
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.SettingViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.LoginCheckUseCaseEventListenerImpl
import net.matsudamper.money.frontend.common.viewmodel.root.mail.HomeAddTabScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.RootUsageCalendarPagingModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.RootUsageHostViewModel
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery
import net.matsudamper.money.ui.root.platform.PlatformTools
import net.matsudamper.money.ui.root.viewmodel.LocalViewModelProviders
import net.matsudamper.money.ui.root.viewmodel.ViewModelProviders

private enum class ScopeKey {
    ROOT,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun Content(
    modifier: Modifier = Modifier,
    globalEventSender: EventSender<GlobalEvent>,
    platformToolsProvider: () -> PlatformTools,
    navController: ScreenNavController,
    composeSizeProvider: () -> MutableStateFlow<IntSize> = { MutableStateFlow(IntSize.Zero) },
    onBack: (() -> Unit)? = null,
) {
    val scopedObjectStoreOwner = rememberScopedObjectStoreOwner(ScopeKey.ROOT::class.toString())
    CompositionLocalProvider(
        LocalScopedObjectStore provides scopedObjectStoreOwner.createOrGetScopedObjectStore(ScopeKey.ROOT),
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

        CompositionLocalProvider(
            LocalGlobalEventHandlerLoginCheckUseCaseDelegate provides remember {
                GlobalEventHandlerLoginCheckUseCaseDelegate(
                    useCase = LoginCheckUseCaseImpl(
                        graphqlQuery = GraphqlUserLoginQuery(
                            graphqlClient = koin.get(),
                        ),
                        eventListener = LoginCheckUseCaseEventListenerImpl(
                            navController = navController,
                            globalEventSender = globalEventSender,
                            coroutineScope = rootCoroutineScope,
                        ),
                    ),
                )
            },
            LocalViewModelProviders provides remember {
                ViewModelProviders(
                    koin = koin,
                    navController = navController,
                    rootCoroutineScope = rootCoroutineScope,
                )
            },
        ) {
            val rootViewModel = LocalViewModelProviders.current.rootViewModel()

            val rootUsageHostViewModel = LocalScopedObjectStore.current.putOrGet<RootUsageHostViewModel>(Unit) {
                RootUsageHostViewModel(
                    scopedObjectFeature = it,
                    navController = navController,
                    calendarPagingModel = RootUsageCalendarPagingModel(
                        coroutineScope = rootCoroutineScope,
                        graphqlClient = koin.get<GraphqlClient>(),
                    ),
                )
            }
            val mailScreenViewModel = LocalScopedObjectStore.current.putOrGet<HomeAddTabScreenViewModel>(Unit) {
                HomeAddTabScreenViewModel(
                    scopedObjectFeature = it,
                    navController = navController,
                )
            }
            val settingViewModel = LocalScopedObjectStore.current.putOrGet<SettingViewModel>(Unit) {
                SettingViewModel(
                    scopedObjectFeature = it,
                    globalEventSender = globalEventSender,
                    ioDispatchers = Dispatchers.IO,
                    navController = navController,
                )
            }
            val kakeboScaffoldListener: KakeboScaffoldListener = remember {
                object : KakeboScaffoldListener {
                    override fun onClickTitle() {
                        navController.navigateToHome()
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
                platformToolsProvider,
            ) {
                ViewModelEventHandlers(
                    navController = navController,
                    globalEventSender = globalEventSender,
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
            LaunchedEffect(navController.currentBackstackEntry) {
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
                val paddingValues = rememberUpdatedState(paddingValues)
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val movableRoot = remember {
                        movableContentOf { current: ScreenStructure.Root ->
                            RootScreenContainer(
                                current = current,
                                navController = navController,
                                settingViewModel = settingViewModel,
                                mailScreenViewModel = mailScreenViewModel,
                                rootUsageHostViewModel = rootUsageHostViewModel,
                                viewModelEventHandlers = viewModelEventHandlers,
                                rootCoroutineScope = rootCoroutineScope,
                                globalEventSender = globalEventSender,
                                globalEvent = globalEvent,
                            )
                        }
                    }
                    NavHost(
                        navController = navController,
                        onBack = onBack ?: {
                            if (navController.canGoBack) {
                                navController.back()
                            }
                        },
                        entryProvider = entryProvider(
                            fallback = { unknownScreen ->
                                when (unknownScreen) {
                                    is ScreenStructure.Root -> {
                                        NavEntry(
                                            key = unknownScreen,
                                            contentKey = unknownScreen,
                                        ) {
                                            movableRoot(unknownScreen)
                                        }
                                    }

                                    else -> throw IllegalStateException("Unknown screen $unknownScreen")
                                }
                            },
                        ) {
                            addEntryProvider<ScreenStructure.Login> {
                                LoginScreenContainer(
                                    navController = navController,
                                    globalEventSender = globalEventSender,
                                    windowInsets = paddingValues.value,
                                )
                            }
                            addEntryProvider<ScreenStructure.Admin> {
                                AdminContainer(
                                    windowInsets = paddingValues.value,
                                )
                            }
                            addEntryProvider<ScreenStructure.NotFound> {
                                NotFoundScreen(
                                    paddingValues = paddingValues.value,
                                )
                            }
                            addEntryProvider<ScreenStructure.AddMoneyUsage> { current ->
                                AddMoneyUsageScreenContainer(
                                    rootCoroutineScope = rootCoroutineScope,
                                    current = current,
                                    viewModelEventHandlers = viewModelEventHandlers,
                                    windowInsets = paddingValues.value,
                                )
                            }

                            addEntryProvider<ScreenStructure.ImportedMail> { current ->
                                ImportedMailScreenContainer(
                                    current = current,
                                    viewModelEventHandlers = viewModelEventHandlers,
                                    windowInsets = paddingValues.value,
                                )
                            }

                            addEntryProvider<ScreenStructure.ImportedMailHTML> { current ->
                                ImportedMailHtmlContainer(
                                    current = current,
                                    viewModelEventHandlers = viewModelEventHandlers,
                                    kakeboScaffoldListener = kakeboScaffoldListener,
                                    windowInsets = paddingValues.value,
                                )
                            }

                            addEntryProvider<ScreenStructure.ImportedMailPlain> { current ->
                                ImportedMailPlainScreenContainer(
                                    screen = current,
                                    viewModelEventHandlers = viewModelEventHandlers,
                                    kakeboScaffoldListener = kakeboScaffoldListener,
                                    windowInsets = paddingValues.value,
                                )
                            }

                            addEntryProvider<ScreenStructure.MoneyUsage> { current ->
                                MoneyUsageContainer(
                                    screen = current,
                                    viewModelEventHandlers = viewModelEventHandlers,
                                    kakeboScaffoldListener = kakeboScaffoldListener,
                                    windowInsets = paddingValues.value,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

private inline fun <reified K : IScreenStructure> EntryProviderScope<IScreenStructure>.addEntryProvider(
    crossinline content: @Composable (current: K) -> Unit,
) {
    addEntryProvider(
        clazz = K::class,
        clazzContentKey = { it },
    ) { current ->
        content(current)
    }
}
