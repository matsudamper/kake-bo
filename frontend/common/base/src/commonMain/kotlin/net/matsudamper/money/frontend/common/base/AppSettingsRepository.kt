package net.matsudamper.money.frontend.common.base

import kotlinx.coroutines.flow.Flow

public interface AppSettingsRepository {
    public fun notificationUsageAutoAddEnabled(filterId: String): Flow<Boolean>

    public fun setNotificationUsageAutoAddEnabled(filterId: String, value: Boolean)
}
