package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.base.notification.EmptyNotificationUsageAccessGateway
import net.matsudamper.money.frontend.common.base.notification.EmptyNotificationUsageRepository
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageAccessGateway
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRepository
import net.matsudamper.money.frontend.common.ui.screen.root.add.NotificationUsageListScreen
import net.matsudamper.money.frontend.common.viewmodel.root.add.NotificationUsageViewModel

@Composable
internal fun NotificationUsageScreenContainer(
    current: ScreenStructure.Root.Add,
    viewModelEventHandlers: ViewModelEventHandlers,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val mode = when (current) {
        ScreenStructure.Root.Add.NotificationUsage -> NotificationUsageViewModel.Mode.AddFromNotification
        ScreenStructure.Root.Add.NotificationUsageDebug -> NotificationUsageViewModel.Mode.NotificationList
        else -> error("Unsupported screen: $current")
    }
    val repository = runCatching { koin.get<NotificationUsageRepository>() }
        .getOrElse { EmptyNotificationUsageRepository }
    val accessGateway = runCatching { koin.get<NotificationUsageAccessGateway>() }
        .getOrElse { EmptyNotificationUsageAccessGateway }
    val viewModel = LocalScopedObjectStore.current.putOrGet(current) {
        NotificationUsageViewModel(
            scopedObjectFeature = it,
            mode = mode,
            repository = repository,
            accessGateway = accessGateway,
        )
    }
    LaunchedEffect(viewModelEventHandlers, viewModel.eventHandler) {
        viewModelEventHandlers.handleNotificationUsage(viewModel.eventHandler)
    }
    NotificationUsageListScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = viewModel.uiStateFlow.collectAsState().value,
        windowInsets = windowInsets,
    )
}
