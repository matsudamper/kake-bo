package net.matsudamper.money.frontend.common.viewmodel.root.mail

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.root.mail.HomeAddTabScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.add.HomeAddExtensionEntryProvider

public class HomeAddTabScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val additionalEntryProviders: List<HomeAddExtensionEntryProvider>,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val navigateEventSender = EventSender<NavigateEvent>()
    public val navigateEventHandler: EventHandler<NavigateEvent> = navigateEventSender.asHandler()

    public val uiStateFlow: StateFlow<HomeAddTabScreenUiState> = MutableStateFlow(
        HomeAddTabScreenUiState(
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navController.navigateToHome()
                }
            },
            items = listOf<HomeAddTabScreenUiState.Item>().toImmutableList(),
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        items = createItems(viewModelState).toImmutableList(),
                    )
                }
            }
        }
    }.asStateFlow()

    public fun updateScreenStructure(structure: ScreenStructure.Root.Add) {
        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                screenStructure = structure,
                lastImportedMailStructure = (structure as? ScreenStructure.Root.Add.Imported)
                    ?: viewModelState.lastImportedMailStructure,
                lastImportMailStructure = (structure as? ScreenStructure.Root.Add.Import)
                    ?: viewModelState.lastImportMailStructure,
            )
        }
    }

    public fun requestNavigate() {
        viewModelScope.launch {
            navigateEventSender.send {
                it.navigate(
                    ScreenStructure.Root.Add.Root,
                )
            }
        }
    }

    public interface NavigateEvent {
        public fun navigate(structure: ScreenStructure)
    }

    private fun createItems(viewModelState: ViewModelState): List<HomeAddTabScreenUiState.Item> {
        val defaultItems = buildList {
            add(
                OrderedItem(
                    order = 10,
                    item = HomeAddTabScreenUiState.Item(
                        title = "メールのインポート",
                        icon = HomeAddTabScreenUiState.Icon.ImportMail,
                        listener = object : HomeAddTabScreenUiState.ItemListener {
                            override fun onClick() {
                                viewModelScope.launch {
                                    navigateEventSender.send {
                                        it.navigate(
                                            viewModelState.lastImportMailStructure
                                                ?: ScreenStructure.Root.Add.Import,
                                        )
                                    }
                                }
                            }
                        },
                    ),
                ),
            )
            add(
                OrderedItem(
                    order = 20,
                    item = HomeAddTabScreenUiState.Item(
                        title = "インポートされたメールから追加",
                        icon = HomeAddTabScreenUiState.Icon.ImportedMail,
                        listener = object : HomeAddTabScreenUiState.ItemListener {
                            override fun onClick() {
                                viewModelScope.launch {
                                    navigateEventSender.send {
                                        it.navigate(
                                            viewModelState.lastImportedMailStructure
                                                ?: ScreenStructure.Root.Add.Imported(isLinked = false, text = null),
                                        )
                                    }
                                }
                            }
                        },
                    ),
                ),
            )
            add(
                OrderedItem(
                    order = 30,
                    item = HomeAddTabScreenUiState.Item(
                        title = "プリセットから追加",
                        icon = HomeAddTabScreenUiState.Icon.Preset,
                        listener = object : HomeAddTabScreenUiState.ItemListener {
                            override fun onClick() {
                                viewModelScope.launch {
                                    navigateEventSender.send {
                                        it.navigate(ScreenStructure.Root.Add.Preset)
                                    }
                                }
                            }
                        },
                    ),
                ),
            )
            additionalEntryProviders.mapTo(this) { provider ->
                val entry = provider.createEntry()
                OrderedItem(
                    order = entry.order,
                    item = HomeAddTabScreenUiState.Item(
                        title = entry.title,
                        icon = entry.icon.toUiIcon(),
                        listener = object : HomeAddTabScreenUiState.ItemListener {
                            override fun onClick() {
                                viewModelScope.launch {
                                    navigateEventSender.send {
                                        it.navigate(entry.screenStructure)
                                    }
                                }
                            }
                        },
                    ),
                )
            }
        }
        return defaultItems.sortedBy { it.order }
            .map { it.item }
    }

    private fun HomeAddExtensionEntryProvider.Entry.Icon.toUiIcon(): HomeAddTabScreenUiState.Icon {
        return when (this) {
            HomeAddExtensionEntryProvider.Entry.Icon.Notification -> HomeAddTabScreenUiState.Icon.Notification
        }
    }

    private data class OrderedItem(
        val order: Int,
        val item: HomeAddTabScreenUiState.Item,
    )

    private data class ViewModelState(
        val screenStructure: ScreenStructure.Root.Add? = null,
        val lastImportedMailStructure: ScreenStructure.Root.Add.Imported? = null,
        val lastImportMailStructure: ScreenStructure.Root.Add.Import? = null,
    )
}
