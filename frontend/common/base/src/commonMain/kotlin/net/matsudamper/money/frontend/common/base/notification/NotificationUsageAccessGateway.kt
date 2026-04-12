package net.matsudamper.money.frontend.common.base.notification

import kotlinx.coroutines.flow.Flow

public interface NotificationUsageAccessGateway {
    public fun accessStateFlow(): Flow<NotificationAccessState>

    public fun openAccessSettings()
}
