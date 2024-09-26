package net.matsudamper.money.ui.root

import androidx.compose.material3.SnackbarHostState
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
import net.matsudamper.money.ui.root.platform.PlatformTools

data class ViewModelEventHandlers(
    private val navController: JsScreenNavController,
    private val globalEventSender: EventSender<GlobalEvent>,
    private val rootScreenScaffoldListener: RootScreenScaffoldListener,
    private val platformToolsProvider: () -> PlatformTools,
) {
    suspend fun handleMailImport(handler: EventHandler<MailImportViewModelEvent>) {
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

    suspend fun handleMailLink(handler: EventHandler<MailLinkViewModelEvent>) {
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

    suspend fun handleSetting(handler: EventHandler<SettingViewModel.Event>) {
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
                        platformToolsProvider().urlOpener.open(url)
                    }
                },
            )
        }
    }

    suspend fun handleSettingCategories(handler: EventHandler<SettingCategoriesViewModelEvent>) {
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

    suspend fun handleSettingCategory(handler: EventHandler<SettingCategoryViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : SettingCategoryViewModel.Event {
                },
            )
        }
    }

    suspend fun handleMoneyUsagesList(handler: EventHandler<MoneyUsagesListViewModel.Event>) {
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

    suspend fun handleHomeAddTabScreen(handler: EventHandler<HomeAddTabScreenViewModel.NavigateEvent>) {
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

    suspend fun handleImportedMailScreen(handler: EventHandler<ImportedMailScreenViewModel.Event>) {
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
                        platformToolsProvider().urlOpener.open(url)
                    }

                    override fun copyToClipboard(text: String) {
                        platformToolsProvider().clipboardManager.copy(text)
                    }
                },
            )
        }
    }

    suspend fun handleImportedMailHtml(handler: EventHandler<ImportedMailHtmlViewModel.Event>) {
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

    suspend fun handleSettingMailCategoryFilters(handler: EventHandler<SettingMailCategoryFiltersViewModel.Event>) {
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

    suspend fun handleImportedMailFilterCategory(handler: EventHandler<ImportedMailFilterCategoryViewModel.Event>) {
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

    suspend fun handleAddMoneyUsage(handler: EventHandler<AddMoneyUsageViewModel.Event>) {
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

    suspend fun handleMoneyUsageScreen(handler: EventHandler<MoneyUsageScreenViewModel.Event>) {
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
                        platformToolsProvider().clipboardManager.copy(text)
                    }

                    override fun openUrl(text: String) {
                        platformToolsProvider().urlOpener.open(text)
                    }
                },
            )
        }
    }

    suspend fun handleImportedMailPlain(handler: EventHandler<ImportedMailPlainViewModel.Event>) {
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

    suspend fun handleRootUsageHost(handler: EventHandler<RootUsageHostViewModel.RootNavigationEvent>) {
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

    suspend fun handleMoneyUsagesCalendar(handler: EventHandler<MoneyUsagesCalendarViewModel.Event>) {
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

    suspend fun handleRootHomeTabPeriodAllContent(handler: EventHandler<RootHomeTabPeriodAllContentViewModel.Event>) {
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

    suspend fun handleRootHomeTabPeriodCategoryContent(handler: EventHandler<RootHomeTabPeriodCategoryContentViewModel.Event>) {
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

    suspend fun handleRootHomeMonthlyCategoryScreen(handler: EventHandler<RootHomeMonthlyCategoryScreenViewModel.Event>) {
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

    suspend fun handleRootHomeMonthlyScreen(handler: EventHandler<RootHomeMonthlyScreenViewModel.Event>) {
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

    suspend fun handleLoginSetting(handler: EventHandler<LoginSettingViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : LoginSettingViewModel.Event {
                    override fun navigate(structure: ScreenStructure) {
                        navController.navigate(structure)
                    }

                    override fun showToast(text: String) {
                        platformToolsProvider().applicationNotificationManager.notify(text)
                    }
                },
            )
        }
    }

    suspend fun handleApiSettingScreen(
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
                        platformToolsProvider().applicationNotificationManager.notify(text)
                    }

                    override fun copyToClipboard(token: String) {
                        platformToolsProvider().clipboardManager.copy(token)
                    }

                    override suspend fun showSnackbar(text: String) {
                        snackbarHostState.showSnackbar(text)
                    }
                },
            )
        }
    }
}
