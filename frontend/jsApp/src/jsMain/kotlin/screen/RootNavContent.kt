package screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import ImportMailScreenUiState
import event.ViewModelEventHandlers
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.root.RootListScreen
import net.matsudamper.money.frontend.common.ui.screen.root.RootListScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.RootScreen
import net.matsudamper.money.frontend.common.ui.screen.root.mail.ImportedMailListScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.mail.HomeMailTabScreen
import net.matsudamper.money.frontend.common.ui.screen.root.mail.HomeMailTabScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.home.HomeGraphqlApi
import net.matsudamper.money.frontend.common.viewmodel.root.home.HomeViewModel

@Composable
internal fun RootNavContent(
    tabHolder: SaveableStateHolder,
    current: ScreenStructure.Root,
    rootScreenScaffoldListener: RootScreenScaffoldListener,
    viewModelEventHandlers: ViewModelEventHandlers,
    globalEvent: GlobalEvent,
    rootCoroutineScope: CoroutineScope,
    globalEventSender: EventSender<GlobalEvent>,
    loginCheckUseCase: LoginCheckUseCase,
    homeMailTabScreenUiStateProvider: @Composable (ScreenStructure.Root.Mail) -> HomeMailTabScreenUiState,
    listUiStateProvider: @Composable (ScreenStructure.Root.List) -> RootListScreenUiState,
    importMailScreenUiStateProvider: @Composable (ScreenStructure.Root.Mail.Imported) -> ImportedMailListScreenUiState,
    importMailLinkScreenUiStateProvider: @Composable (ScreenStructure.Root.Mail.Import) -> ImportMailScreenUiState,
) {
    when (current) {
        is ScreenStructure.Root.Home -> {
            tabHolder.SaveableStateProvider(current::class.toString()) {
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

        is ScreenStructure.Root.List -> {
            tabHolder.SaveableStateProvider(current::class.toString()) {
                val uiState = listUiStateProvider(current)
                RootListScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState,
                    listener = rootScreenScaffoldListener,
                )
            }
        }

        is ScreenStructure.Root.Mail -> {
            tabHolder.SaveableStateProvider(current::class.toString()) {
                val uiState = homeMailTabScreenUiStateProvider(current)
                HomeMailTabScreen(
                    screenStructure = current,
                    uiState = uiState,
                    importMailScreenUiStateProvider = {
                        importMailLinkScreenUiStateProvider(it)
                    },
                    importedImportMailScreenUiStateProvider = {
                        importMailScreenUiStateProvider(it)
                    },
                    rootScreenScaffoldListener = rootScreenScaffoldListener,
                )
            }
        }

        is ScreenStructure.Root.Settings -> {
            tabHolder.SaveableStateProvider(current::class.toString()) {
                SettingNavContent(
                    state = current,
                    globalEventSender = globalEventSender,
                    globalEvent = globalEvent,
                    rootScreenScaffoldListener = rootScreenScaffoldListener,
                    viewModelEventHandlers = viewModelEventHandlers,
                    rootCoroutineScope = rootCoroutineScope,
                )
            }
        }
    }
}
