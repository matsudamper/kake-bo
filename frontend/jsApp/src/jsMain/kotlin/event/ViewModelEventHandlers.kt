package event

import androidx.compose.material3.SnackbarHostState
import kotlinx.browser.window
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.base.nav.user.JsScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.viewmodel.addmoneyusage.AddMoneyUsageViewModel
import net.matsudamper.money.frontend.common.viewmodel.importedmail.html.ImportedMailHtmlViewModel
import net.matsudamper.money.frontend.common.viewmodel.importedmail.plain.ImportedMailPlainViewModel
import net.matsudamper.money.frontend.common.viewmodel.importedmail.root.ImportedMailScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.moneyusage.MoneyUsageScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.SettingViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabPeriodAllContentViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabPeriodCategoryContentViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.monthly.RootHomeMonthlyScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.monthly.category.RootHomeMonthlyCategoryScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.mail.HomeAddTabScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.mail.MailImportViewModelEvent
import net.matsudamper.money.frontend.common.viewmodel.root.mail.MailLinkViewModelEvent
import net.matsudamper.money.frontend.common.viewmodel.root.settings.api.ApiSettingScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilter.ImportedMailFilterCategoryViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilters.SettingMailCategoryFiltersViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.settings.login.LoginSettingViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.MoneyUsagesCalendarViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.MoneyUsagesListViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.RootUsageHostViewModel
import net.matsudamper.money.frontend.common.viewmodel.settings.SettingCategoriesViewModelEvent
import net.matsudamper.money.frontend.common.viewmodel.settings.SettingCategoryViewModel

data class ViewModelEventHandlers(
    private val navController: JsScreenNavController,
    private val globalEventSender: EventSender<GlobalEvent>,
    private val rootScreenScaffoldListener: RootScreenScaffoldListener,
) {
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
                        navController.navigate(ScreenStructure.Root.Add.Imported(isLinked = isLinked))
                    }

                    override fun navigateToMailDetail(id: ImportedMailId) {
                        navController.navigate(ScreenStructure.ImportedMail(id = id))
                    }

                    override fun navigateToMailContent(id: ImportedMailId) {
                        navController.navigate(ScreenStructure.ImportedMailHTML(id = id))
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

                    override fun navigateToApiSetting() {
                        navController.navigate(ScreenStructure.Root.Settings.Api)
                    }

                    override fun navigate(structure: ScreenStructure) {
                        navController.navigate(structure)
                    }

                    override fun open(url: String) {
                        window.open(url)
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

    suspend fun handle(handler: EventHandler<MoneyUsagesListViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : MoneyUsagesListViewModel.Event {
                    override fun navigate(screenStructure: ScreenStructure) {
                        navController.navigate(screenStructure)
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<HomeAddTabScreenViewModel.NavigateEvent>) {
        coroutineScope {
            handler.collect(
                object : HomeAddTabScreenViewModel.NavigateEvent {
                    override fun navigate(structure: ScreenStructure) {
                        navController.navigate(structure)
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<ImportedMailScreenViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : ImportedMailScreenViewModel.Event {
                    override fun navigateToBack() {
                        navController.back()
                    }

                    override fun navigateToHome() {
                        navController.navigateToHome()
                    }

                    override fun navigate(screenStructure: ScreenStructure) {
                        navController.navigate(screenStructure)
                    }

                    override fun openWeb(url: String) {
                        window.open(url)
                    }

                    override fun copyToClipboard(text: String) {
                        window.navigator.clipboard.writeText(text)
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<ImportedMailHtmlViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : ImportedMailHtmlViewModel.Event {
                    override fun backRequest() {
                        navController.back()
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<SettingMailCategoryFiltersViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : SettingMailCategoryFiltersViewModel.Event {
                    override fun navigate(structure: ScreenStructure) {
                        navController.navigate(structure)
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<ImportedMailFilterCategoryViewModel.Event>) {
        coroutineScope {
            val scope = this
            handler.collect(
                object : ImportedMailFilterCategoryViewModel.Event {
                    override fun showNativeAlert(text: String) {
                        scope.launch {
                            globalEventSender.send { it.showNativeNotification(text) }
                        }
                    }

                    override fun navigate(structure: ScreenStructure) {
                        navController.navigate(structure)
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<AddMoneyUsageViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : AddMoneyUsageViewModel.Event {
                    override fun navigate(structure: ScreenStructure) {
                        navController.navigate(structure)
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<MoneyUsageScreenViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : MoneyUsageScreenViewModel.Event {
                    override fun navigate(structure: ScreenStructure) {
                        navController.navigate(structure)
                    }

                    override fun navigateBack() {
                        navController.back()
                    }

                    override fun copyUrl(text: String) {
                        window.navigator.clipboard.writeText(text)
                    }

                    override fun openUrl(text: String) {
                        window.open(text)
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<ImportedMailPlainViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : ImportedMailPlainViewModel.Event {
                    override fun backRequest() {
                        navController.back()
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<RootUsageHostViewModel.RootNavigationEvent>) {
        coroutineScope {
            handler.collect(
                object : RootUsageHostViewModel.RootNavigationEvent {
                    override fun navigate(screenStructure: ScreenStructure) {
                        navController.navigate(screenStructure)
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<MoneyUsagesCalendarViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : MoneyUsagesCalendarViewModel.Event {
                    override fun navigate(screenStructure: ScreenStructure) {
                        navController.navigate(screenStructure)
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<RootHomeTabPeriodAllContentViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : RootHomeTabPeriodAllContentViewModel.Event {
                    override fun navigate(screen: ScreenStructure) {
                        navController.navigate(screen)
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<RootHomeTabScreenViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : RootHomeTabScreenViewModel.Event {
                    override fun navigate(screen: ScreenStructure) {
                        navController.navigate(screen)
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<RootHomeTabPeriodCategoryContentViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : RootHomeTabPeriodCategoryContentViewModel.Event {
                    override fun navigate(screen: ScreenStructure) {
                        navController.navigate(screen)
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<RootHomeMonthlyCategoryScreenViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : RootHomeMonthlyCategoryScreenViewModel.Event {
                    override fun navigate(screen: ScreenStructure) {
                        navController.navigate(screen)
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<RootHomeMonthlyScreenViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : RootHomeMonthlyScreenViewModel.Event {
                    override fun navigate(screen: ScreenStructure) {
                        navController.navigate(screen)
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<LoginSettingViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : LoginSettingViewModel.Event {
                    override fun navigate(structure: ScreenStructure) {
                        navController.navigate(structure)
                    }

                    override fun showToast(text: String) {
                        window.alert(text)
                    }
                },
            )
        }
    }

    suspend fun handle(
        eventHandler: EventHandler<ApiSettingScreenViewModel.Event>,
        snackbarHostState: SnackbarHostState,
    ) {
        coroutineScope {
            eventHandler.collect(
                object : ApiSettingScreenViewModel.Event {
                    override fun navigate(structure: ScreenStructure) {
                        navController.navigate(structure)
                    }

                    override fun showToast(text: String) {
                        window.alert(text)
                    }

                    override fun copyToClipboard(token: String) {
                        window.navigator.clipboard.writeText(token)
                    }

                    override suspend fun showSnackbar(text: String) {
                        snackbarHostState.showSnackbar(text)
                    }
                },
            )
        }
    }
}
