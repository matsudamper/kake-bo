package net.matsudamper.money.ui.root.viewmodel

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
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
    private val navController: ScreenNavController,
    private val rootCoroutineScope: CoroutineScope,
) {
    @Composable
    fun rootViewModel(): RootViewModel {
        return LocalScopedObjectStore.current.putOrGet(Unit) { feature ->
            RootViewModel(
                loginCheckUseCase = koin.get(),
                scopedObjectFeature = feature,
                navController = navController,
            )
        }
    }

    @Composable
    fun moneyUsagesCalendarViewModel(
        coroutineScope: CoroutineScope,
        rootUsageHostViewModel: RootUsageHostViewModel,
        yearMonth: ScreenStructure.Root.Usage.Calendar.YearMonth?,
    ): MoneyUsagesCalendarViewModel {
        return LocalScopedObjectStore.current.putOrGet(Unit) { feature ->
            MoneyUsagesCalendarViewModel(
                scopedObjectFeature = feature,
                rootUsageHostViewModel = rootUsageHostViewModel,
                yearMonth = yearMonth,
            )
        }
    }

    @Composable
    fun moneyUsagesListViewModel(
        coroutineScope: CoroutineScope,
        rootUsageHostViewModel: RootUsageHostViewModel,
    ): MoneyUsagesListViewModel {
        return LocalScopedObjectStore.current.putOrGet(Unit) { feature ->
            MoneyUsagesListViewModel(
                scopedObjectFeature = feature,
                rootUsageHostViewModel = rootUsageHostViewModel,
                graphqlClient = koin.get(),
            )
        }
    }

    @Composable
    fun mailImportViewModel(): MailImportViewModel {
        return LocalScopedObjectStore.current.putOrGet(Unit) { feature ->
            MailImportViewModel(
                scopedObjectFeature = feature,
                ioDispatcher = Dispatchers.IO,
                graphqlApi = MailImportScreenGraphqlApi(
                    graphqlClient = koin.get(),
                ),
                loginCheckUseCase = koin.get(),
                navController = navController,
            )
        }
    }

    @Composable
    fun importedMailListViewModel(): ImportedMailListViewModel {
        return LocalScopedObjectStore.current.putOrGet(Unit) { feature ->
            ImportedMailListViewModel(
                scopedObjectFeature = feature,
                ioDispatcher = Dispatchers.IO,
                graphqlApi = MailLinkScreenGraphqlApi(
                    graphqlClient = koin.get(),
                ),
                navController = navController,
            )
        }
    }

    @Composable
    fun rootHomeTabPeriodAllContentViewModel(): RootHomeTabPeriodAllContentViewModel {
        return createViewModelProvider {
            RootHomeTabPeriodAllContentViewModel(
                scopedObjectFeature = it,
                api = RootHomeTabScreenApi(
                    graphqlClient = koin.get(),
                ),
                loginCheckUseCase = koin.get(),
                graphqlClient = koin.get(),
                navController = navController,
            )
        }.get()
    }
}
