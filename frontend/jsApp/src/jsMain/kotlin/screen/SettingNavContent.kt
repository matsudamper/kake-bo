package screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import event.ViewModelEventHandlers
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.root.RootSettingScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.settings.ImapConfigScreen
import net.matsudamper.money.frontend.common.ui.screen.root.settings.SettingCategoriesScreen
import net.matsudamper.money.frontend.common.ui.screen.root.settings.SettingCategoryScreen
import net.matsudamper.money.frontend.common.ui.screen.root.settings.SettingMailCategoryFilterScreen
import net.matsudamper.money.frontend.common.ui.screen.root.settings.SettingRootScreen
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.ImapSettingViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.settings.ImportedMailCategoryFilterScreenPagingModel
import net.matsudamper.money.frontend.common.viewmodel.root.settings.SettingImportedMailCategoryFilterApi
import net.matsudamper.money.frontend.common.viewmodel.root.settings.SettingMailCategoryFilterViewModel
import net.matsudamper.money.frontend.common.viewmodel.settings.SettingCategoriesViewModel
import net.matsudamper.money.frontend.common.viewmodel.settings.SettingCategoryViewModel
import net.matsudamper.money.frontend.common.viewmodel.settings.SettingScreenCategoryApi
import net.matsudamper.money.frontend.graphql.GraphqlUserConfigQuery

@Composable
internal fun SettingNavContent(
    state: ScreenStructure.Root.Settings,
    globalEventSender: EventSender<GlobalEvent>,
    globalEvent: GlobalEvent,
    settingUiStateProvider: @Composable () -> RootSettingScreenUiState,
    viewModelEventHandlers: ViewModelEventHandlers,
    rootScreenScaffoldListener: RootScreenScaffoldListener,
) {
    val coroutineScope = rememberCoroutineScope()
    val holder = rememberSaveableStateHolder()
    when (state) {
        ScreenStructure.Root.Settings.Root -> {
            holder.SaveableStateProvider(state::class.toString()) {
                SettingRootScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = settingUiStateProvider(),
                    listener = rootScreenScaffoldListener,
                )
            }
        }

        ScreenStructure.Root.Settings.Categories -> {
            holder.SaveableStateProvider(state::class.toString()) {
                val viewModel = remember {
                    SettingCategoriesViewModel(
                        coroutineScope = coroutineScope,
                        api = SettingScreenCategoryApi(),
                    )
                }

                LaunchedEffect(viewModel.viewModelEventHandler) {
                    viewModelEventHandlers.handle(viewModel.viewModelEventHandler)
                }
                LaunchedEffect(globalEvent, viewModel.globalEventHandler) {
                    viewModel.globalEventHandler.collect(globalEvent)
                }

                SettingCategoriesScreen(
                    rootScreenScaffoldListener = rootScreenScaffoldListener,
                    uiState = viewModel.uiState.collectAsState().value,
                )
            }
        }

        is ScreenStructure.Root.Settings.Category -> {
            holder.SaveableStateProvider(state::class.toString()) {
                val viewModel = remember {
                    SettingCategoryViewModel(
                        coroutineScope = coroutineScope,
                        api = SettingScreenCategoryApi(),
                        categoryId = state.id,
                    )
                }
                LaunchedEffect(viewModel.viewModelEventHandler) {
                    viewModelEventHandlers.handle(viewModel.viewModelEventHandler)
                }
                LaunchedEffect(viewModel.globalEventHandler) {
                    viewModel.globalEventHandler.collect(globalEvent)
                }
                SettingCategoryScreen(
                    rootScreenScaffoldListener = rootScreenScaffoldListener,
                    uiState = viewModel.uiState.collectAsState().value,
                )
            }
        }

        ScreenStructure.Root.Settings.Imap -> {
            holder.SaveableStateProvider(state::class.toString()) {
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
        }

        ScreenStructure.Root.Settings.MailCategoryFilters -> {
            holder.SaveableStateProvider(state::class.toString()) {
                val viewModel = remember(coroutineScope) {
                    SettingMailCategoryFilterViewModel(
                        coroutineScope = coroutineScope,
                        pagingModel = ImportedMailCategoryFilterScreenPagingModel(
                            coroutineScope = coroutineScope,
                        ),
                        api = SettingImportedMailCategoryFilterApi()
                    )
                }
                LaunchedEffect(viewModel.eventHandler, viewModelEventHandlers) {
                    viewModelEventHandlers.handle(viewModel.eventHandler)
                }
                SettingMailCategoryFilterScreen(
                    modifier = Modifier.fillMaxSize(),
                    uiState = viewModel.uiStateFlow.collectAsState().value,
                    listener = rootScreenScaffoldListener,
                )
            }
        }

        is ScreenStructure.Root.Settings.MailCategoryFilter -> {
            holder.SaveableStateProvider(state::class.toString()) {
                Text("TODO")
            }
        }
    }
}
