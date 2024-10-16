package net.matsudamper.money.ui.root.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavControllerImpl
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.viewmodel.root.RootViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabPeriodAllContentViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabScreenApi
import net.matsudamper.money.frontend.common.viewmodel.root.mail.ImportedMailListViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.mail.MailImportViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.MoneyUsagesCalendarViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.MoneyUsagesListViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.usage.RootUsageHostViewModel
import net.matsudamper.money.frontend.graphql.MailImportScreenGraphqlApi
import net.matsudamper.money.frontend.graphql.MailLinkScreenGraphqlApi
import org.koin.core.Koin

internal class ViewModelProviders(
    private val koin: Koin,
    private val navController: ScreenNavControllerImpl,
    private val rootCoroutineScope: CoroutineScope,
) {
    @Composable
    fun rootViewModel(): RootViewModel {
        return createViewModelProvider {
            RootViewModel(
                loginCheckUseCase = koin.get(),
                viewModelFeature = it,
                navController = navController,
            )
        }.get()
    }

    @Composable
    fun moneyUsagesCalendarViewModel(
        coroutineScope: CoroutineScope,
        rootUsageHostViewModel: RootUsageHostViewModel,
        yearMonth: ScreenStructure.Root.Usage.Calendar.YearMonth?,
    ): MoneyUsagesCalendarViewModel {
        return createViewModelProvider {
            MoneyUsagesCalendarViewModel(
                viewModelFeature = it,
                rootUsageHostViewModel = rootUsageHostViewModel,
                yearMonth = yearMonth,
            )
        }.get()
    }

    @Composable
    fun moneyUsagesListViewModel(
        coroutineScope: CoroutineScope,
        rootUsageHostViewModel: RootUsageHostViewModel,
    ): MoneyUsagesListViewModel {
        return createViewModelProvider {
            MoneyUsagesListViewModel(
                viewModelFeature = it,
                rootUsageHostViewModel = rootUsageHostViewModel,
                graphqlClient = koin.get(),
            )
        }.get()
    }

    @Composable
    fun mailImportViewModel(): MailImportViewModel {
        val coroutineScope = rememberCoroutineScope()
        return createViewModelProvider {
            MailImportViewModel(
                viewModelFeature = it,
                ioDispatcher = Dispatchers.IO,
                graphqlApi = MailImportScreenGraphqlApi(
                    graphqlClient = koin.get(),
                ),
                loginCheckUseCase = koin.get(),
            )
        }.get()
    }

    @Composable
    fun importedMailListViewModel(): ImportedMailListViewModel {
        val coroutineScope = rememberCoroutineScope()
        return createViewModelProvider {
            ImportedMailListViewModel(
                viewModelFeature = it,
                ioDispatcher = Dispatchers.IO,
                graphqlApi = MailLinkScreenGraphqlApi(
                    graphqlClient = koin.get(),
                ),
            )
        }.get()
    }

    @Composable
    fun rootHomeTabPeriodAllContentViewModel(): RootHomeTabPeriodAllContentViewModel {
        return createViewModelProvider {
            RootHomeTabPeriodAllContentViewModel(
                viewModelFeature = it,
                api = RootHomeTabScreenApi(
                    graphqlClient = koin.get(),
                ),
                loginCheckUseCase = koin.get(),
                graphqlClient = koin.get(),
            )
        }.get()
    }
}
