package screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import event.ViewModelEventHandlers
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.settings.ImapConfigScreen
import net.matsudamper.money.frontend.common.ui.screen.settings.RootSettingScreen
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.ImapSettingViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.SettingViewModel
import net.matsudamper.money.frontend.graphql.GraphqlUserConfigQuery


@Composable
internal fun SettingNavContent(
    state: ScreenStructure.Root.Settings,
    rootCoroutineScope: CoroutineScope,
    globalEventSender: EventSender<GlobalEvent>,
    viewModelEventHandlers: ViewModelEventHandlers,
    rootScreenScaffoldListener: RootScreenScaffoldListener,
) {
    val coroutineScope = rememberCoroutineScope()
    val settingViewModel = remember {
        SettingViewModel(
            coroutineScope = rootCoroutineScope,
            globalEventSender = globalEventSender,
            ioDispatchers = Dispatchers.Unconfined,
        )
    }
    when (state) {
        ScreenStructure.Root.Settings.Root -> {
            LaunchedEffect(settingViewModel.eventHandler) {
                viewModelEventHandlers.handle(settingViewModel.eventHandler)
            }
            RootSettingScreen(
                modifier = Modifier.fillMaxSize(),
                uiState = settingViewModel.uiState.collectAsState().value,
                listener = rootScreenScaffoldListener,
            )
        }

        ScreenStructure.Root.Settings.Category -> {
            Text(state.direction.placeholderUrl)
        }

        ScreenStructure.Root.Settings.CategoryId -> {
            Text(state.direction.placeholderUrl)
        }

        ScreenStructure.Root.Settings.Imap -> {
            val viewModel = remember {
                ImapSettingViewModel(
                    coroutineScope = coroutineScope,
                    graphqlQuery = GraphqlUserConfigQuery(),
                    globalEventSender = globalEventSender,
                    ioDispatchers = Dispatchers.Unconfined,
                )
            }

            ImapConfigScreen(
                uiState = viewModel.uiState.collectAsState().value,
                listener = rootScreenScaffoldListener,
            )
        }

        ScreenStructure.Root.Settings.SubCategory -> {
            Text(state.direction.placeholderUrl)
        }

        is ScreenStructure.Root.Settings.SubCategoryId -> {
            Column {
                Text(state.direction.placeholderUrl)
                Text("id=${state.id}")
            }
        }
    }
}
