package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.base.AppSettingsRepository
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.ui.screen.root.add.NotificationUsageFilterListScreen
import net.matsudamper.money.frontend.common.viewmodel.root.add.NotificationUsageFilterListViewModel

@Composable
internal fun NotificationUsageFilterListScreenContainer(
    navController: ScreenNavController,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val appSettingsRepository = koin.get<AppSettingsRepository>()
    val parsers = runCatching { koin.getAll<NotificationUsageParser>() }
        .getOrDefault(emptyList())
    val viewModel = LocalScopedObjectStore.current.putOrGet<NotificationUsageFilterListViewModel>(Unit) {
        NotificationUsageFilterListViewModel(
            scopedObjectFeature = it,
            appSettingsRepository = appSettingsRepository,
            parsers = parsers,
            navController = navController,
        )
    }
    NotificationUsageFilterListScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = viewModel.uiStateFlow.collectAsState().value,
        windowInsets = windowInsets,
    )
}
