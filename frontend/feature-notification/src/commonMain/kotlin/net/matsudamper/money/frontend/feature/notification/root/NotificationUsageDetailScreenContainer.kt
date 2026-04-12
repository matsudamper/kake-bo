package net.matsudamper.money.frontend.feature.notification.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRepository
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.feature.notification.ui.NotificationUsageDetailScreen
import net.matsudamper.money.frontend.feature.notification.viewmodel.NotificationUsageDetailViewModel
import net.matsudamper.money.frontend.graphql.GraphqlClient

@Composable
public fun NotificationUsageDetailScreenContainer(
    current: ScreenStructure.NotificationUsageDetail,
    repository: NotificationUsageRepository,
    graphqlClient: GraphqlClient,
    eventHandlerCollector: suspend (EventHandler<NotificationUsageDetailViewModel.Event>) -> Unit,
    windowInsets: PaddingValues,
) {
    val viewModel = LocalScopedObjectStore.current.putOrGet(current.notificationUsageKey) {
        NotificationUsageDetailViewModel(
            scopedObjectFeature = it,
            notificationUsageKey = current.notificationUsageKey,
            repository = repository,
            graphqlClient = graphqlClient,
        )
    }

    LaunchedEffect(eventHandlerCollector, viewModel.eventHandler) {
        eventHandlerCollector(viewModel.eventHandler)
    }

    NotificationUsageDetailScreen(
        modifier = Modifier.fillMaxSize(),
        uiState = viewModel.uiStateFlow.collectAsState().value,
        windowInsets = windowInsets,
    )
}
