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
import net.matsudamper.money.frontend.feature.notification.ui.NotificationUsageFilterListScreen
import net.matsudamper.money.frontend.feature.notification.viewmodel.NotificationUsageFilterListViewModel

@Composable
internal fun NotificationUsageFilterListScreenContainer(
    navController: ScreenNavController,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val viewModel = LocalScopedObjectStore.current.putOrGet<NotificationUsageFilterListViewModel>(Unit) {
        NotificationUsageFilterListViewModel(
            scopedObjectFeature = it,
            appSettingsRepository = koin.get<AppSettingsRepository>(),
            parsers = runCatching { koin.getAll<NotificationUsageParser>() }.getOrDefault(listOf()),
            navController = navController,
        )
    }
    NotificationUsageFilterListScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = viewModel.uiStateFlow.collectAsState().value,
        windowInsets = windowInsets,
    )
}
