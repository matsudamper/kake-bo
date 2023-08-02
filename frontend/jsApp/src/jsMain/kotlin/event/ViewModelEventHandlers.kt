package event

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.mail.MailImportViewModelEvent
import net.matsudamper.money.frontend.common.viewmodel.root.mail.MailLinkViewModelEvent
import net.matsudamper.money.frontend.common.viewmodel.root.SettingViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.HomeViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.list.RootListViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.mail.HomeMailTabScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.settings.SettingCategoriesViewModelEvent
import net.matsudamper.money.frontend.common.viewmodel.settings.SettingCategoryViewModel

data class ViewModelEventHandlers(
    private val navController: ScreenNavController,
    private val globalEventSender: EventSender<GlobalEvent>,
    private val rootScreenScaffoldListener: RootScreenScaffoldListener,
    private val mailViewModelStateFlow: StateFlow<HomeMailTabScreenViewModel.ViewModelState>,
) {
    suspend fun handle(handler: EventHandler<HomeViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : HomeViewModel.Event {
                    override fun navigateToMailImport() {
                        navController.navigate(ScreenStructure.Root.Mail.Import)
                    }

                    override fun navigateToMailLink() {
                        navController.navigate(ScreenStructure.Root.Mail.Imported(isLinked = false))
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<MailImportViewModelEvent>) {
        coroutineScope {
            val scope = this
            handler.collect(
                object : MailImportViewModelEvent {
                    override fun globalToast(message: String) {
                        scope.launch {
                            globalEventSender.send {
                                it.showSnackBar(message)
                            }
                        }
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<MailLinkViewModelEvent>) {
        coroutineScope {
            val scope = this
            handler.collect(
                object : MailLinkViewModelEvent {
                    override fun globalToast(message: String) {
                        scope.launch {
                            globalEventSender.send {
                                it.showSnackBar(message)
                            }
                        }
                    }

                    override fun changeQuery(isLinked: Boolean?) {
                        navController.navigate(ScreenStructure.Root.Mail.Imported(isLinked = isLinked))
                    }

                    override fun navigateToMailDetail(id: ImportedMailId) {
                        navController.navigate(ScreenStructure.Mail(id = id))
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<SettingViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : SettingViewModel.Event {
                    override fun navigateToImapConfig() {
                        navController.navigate(ScreenStructure.Root.Settings.Imap)
                    }

                    override fun navigateToCategoriesConfig() {
                        navController.navigate(ScreenStructure.Root.Settings.Categories)
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<SettingCategoriesViewModelEvent>) {
        coroutineScope {
            handler.collect(
                object : SettingCategoriesViewModelEvent {
                    override fun navigateToCategoryDetail(id: MoneyUsageCategoryId) {
                        navController.navigate(
                            ScreenStructure.Root.Settings.Category(id = id),
                        )
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<SettingCategoryViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : SettingCategoryViewModel.Event {
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<RootListViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : RootListViewModel.Event {
                    override fun navigateToAddMoneyUsage() {
                        navController.navigate(ScreenStructure.AddMoneyUsage)
                    }
                },
            )
        }
    }

    suspend fun handle(
        handler: EventHandler<HomeMailTabScreenViewModel.Event>,
    ) {
        coroutineScope {
            handler.collect(
                object : HomeMailTabScreenViewModel.Event {
                    override fun navigateToImportMail() {
                        navController.navigate(
                            mailViewModelStateFlow.value.lastImportMailStructure
                                ?: ScreenStructure.Root.Mail.Import
                        )
                    }

                    override fun navigateToImportedMail() {
                        navController.navigate(
                            mailViewModelStateFlow.value.lastImportedMailStructure
                                ?: ScreenStructure.Root.Mail.Imported(isLinked = false)
                        )
                    }
                },
            )
        }
    }
}
