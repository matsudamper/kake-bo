package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRepository
import net.matsudamper.money.frontend.common.ui.screen.root.add.NotificationUsageDetailScreen
import net.matsudamper.money.frontend.common.viewmodel.root.add.NotificationUsageDetailViewModel
import net.matsudamper.money.frontend.graphql.GraphqlClient

@Composable
internal fun NotificationUsageDetailScreenContainer(
    current: ScreenStructure.NotificationUsageDetail,
    viewModelEventHandlers: ViewModelEventHandlers,
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val repository = koin.get<NotificationUsageRepository>()
    val viewModel = LocalScopedObjectStore.current.putOrGet(current.notificationUsageKey) {
        NotificationUsageDetailViewModel(
            scopedObjectFeature = it,
            notificationUsageKey = current.notificationUsageKey,
            repository = repository,
            graphqlClient = koin.get<GraphqlClient>(),
        )
    }

    LaunchedEffect(viewModelEventHandlers, viewModel.eventHandler) {
        viewModelEventHandlers.handleNotificationUsageDetail(viewModel.eventHandler)
    }

    NotificationUsageDetailScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = viewModel.uiStateFlow.collectAsState().value,
        windowInsets = windowInsets,
    )
}
