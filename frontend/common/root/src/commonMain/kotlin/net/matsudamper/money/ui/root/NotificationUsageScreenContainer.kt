package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageAccessGateway
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRepository
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.feature.notification.ui.NotificationUsageListScreen
import net.matsudamper.money.frontend.feature.notification.viewmodel.NotificationUsageViewModel

@Composable
internal fun NotificationUsageScreenContainer(
    current: ScreenStructure.Root.Add,
    eventHandlerCollector: suspend (EventHandler<NotificationUsageViewModel.Event>) -> Unit,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val mode = when (current) {
        ScreenStructure.Root.Add.NotificationUsage -> NotificationUsageViewModel.Mode.AddFromNotification
        ScreenStructure.Root.Add.NotificationUsageDebug -> NotificationUsageViewModel.Mode.NotificationList
        else -> error("Unsupported screen: $current")
    }
    val viewModel = LocalScopedObjectStore.current.putOrGet(current) {
        NotificationUsageViewModel(
            scopedObjectFeature = it,
            mode = mode,
            repository = koin.get<NotificationUsageRepository>(),
            accessGateway = koin.get<NotificationUsageAccessGateway>(),
        )
    }
    LaunchedEffect(eventHandlerCollector, viewModel.eventHandler) {
        eventHandlerCollector(viewModel.eventHandler)
    }
    NotificationUsageListScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = viewModel.uiStateFlow.collectAsState().value,
        windowInsets = windowInsets,
    )
}
