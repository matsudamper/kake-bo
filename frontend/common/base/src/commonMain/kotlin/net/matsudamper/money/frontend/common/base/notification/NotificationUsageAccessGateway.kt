package net.matsudamper.money.frontend.common.base.notification

import kotlinx.coroutines.flow.Flow

public interface NotificationUsageAccessGateway {
    public fun accessStateFlow(): Flow<NotificationAccessState>

    public fun openAccessSettings()

    public sealed interface NotificationAccessState {
        public data object Granted : NotificationAccessState

        public data object NotGranted : NotificationAccessState
    }
}
