package net.matsudamper.money.frontend.common.base.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

public interface NotificationUsageAccessGateway {
    public fun accessStateFlow(): Flow<NotificationAccessState>

    public fun openAccessSettings()

    public sealed interface NotificationAccessState {
        public data object Granted : NotificationAccessState

        public data object NotGranted : NotificationAccessState
    }
}

public object EmptyNotificationUsageAccessGateway : NotificationUsageAccessGateway {
    override fun accessStateFlow(): Flow<NotificationUsageAccessGateway.NotificationAccessState> {
        return flowOf(NotificationUsageAccessGateway.NotificationAccessState.NotGranted)
    }

    override fun openAccessSettings() = Unit
}
