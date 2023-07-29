package screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import event.ViewModelEventHandlers
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.RootRegisterScreen
import net.matsudamper.money.frontend.common.ui.screen.RootScreen
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
    rootCoroutineScope: CoroutineScope,
    globalEventSender: EventSender<GlobalEvent>,
    loginCheckUseCase: LoginCheckUseCase,
) {
    when (current) {
        is ScreenStructure.Root.Home -> {
            tabHolder.SaveableStateProvider(ScreenStructure.Root.Home::class.toString()) {
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

        is ScreenStructure.Root.Register -> {
            tabHolder.SaveableStateProvider(ScreenStructure.Root.Register::class.toString()) {
                RootRegisterScreen(
                    modifier = Modifier.fillMaxSize(),
                    listener = rootScreenScaffoldListener,
                )
            }
        }

        is ScreenStructure.Root.Settings -> {
            tabHolder.SaveableStateProvider("ScreenStructure.Root.Settings") {
                SettingNavContent(
                    state = current,
                    globalEventSender = globalEventSender,
                    rootScreenScaffoldListener = rootScreenScaffoldListener,
                    viewModelEventHandlers = viewModelEventHandlers,
                    rootCoroutineScope = rootCoroutineScope,
                )
            }
        }
    }
}
